# 查询链路与索引优化说明

## 1. 场景

工单列表是高频入口：需要联查申请人、部门、当前审批人，并支持状态、类型、关键字筛选与分页。
数据量增长后，主要瓶颈来自：

- 多表联查 + 无索引扫描；
- `LIMIT offset, size` 深分页随着页码增大性能线性下降；
- 列表查询带出 `content` 大字段，增加 IO 与网络传输。

## 2. 优化措施

### 2.1 索引设计

| 索引 | 表 | 作用 |
|---|---|---|
| `uk_order_no` | work_order | 工单号唯一，按单号查询 |
| `uk_request_id` | work_order | 幂等兜底：重复提交在数据库层被拒绝 |
| `idx_status_id (status, id)` | work_order | 按状态筛选 + 按 id 倒序分页 |
| `idx_applicant_id (applicant_id, id)` | work_order | “我的工单” |
| `idx_approver_id (approver_id, id)` | work_order | “待我审批” |
| `idx_dept_id (applicant_dept_id, id)` | work_order | 主管查看本部门 |
| `idx_approval_order (work_order_id, id)` | work_order_approval | 详情页按工单查审批记录 |
| `idx_user_dept_role (dept_id, role, status)` | user | 定位部门主管审批人 |

索引把“过滤列 + 排序列”放在同一棵 B+ 树上，避免 filesort；列表以 `ORDER BY w.id DESC` 为准，
因此联合索引统一以 `id` 收尾。

### 2.2 列表 SQL 只查必要字段

`findPage` / `findCursor` 不查询 `content`、`request_id`、`version` 等大字段或非展示字段，
详情接口 `findDetail` 才查询 `content`。列表平均行宽显著下降。

### 2.3 游标分页替代深分页

传统分页：

```sql
SELECT ... FROM work_order w ... ORDER BY w.id DESC LIMIT 10000, 10;
```

MySQL 需要扫描并丢弃前 10000 行。游标分页：

```sql
SELECT ... FROM work_order w
WHERE w.id < #{lastId}
ORDER BY w.id DESC
LIMIT 10;
```

直接利用主键有序性定位起点，扫描行数与页大小同级，页码越深优势越明显。
接口层通过 `lastId` 参数自动切换：不传为普通分页，传入则走游标模式，并在返回体带回 `nextLastId`。

### 2.4 条件统计

状态、类型、部门统计使用 `GROUP BY` 聚合，走对应联合索引；分页总数由 MyBatis-Plus
自动生成 count 语句，与列表条件保持一致，避免前后端统计口径不一致。

## 3. 验证方式

```sql
-- 查看列表查询执行计划，确认命中 idx_status_id 等索引
EXPLAIN SELECT w.id, w.order_no, w.title, w.status
FROM work_order w
LEFT JOIN user u ON w.applicant_id = u.id
LEFT JOIN dept d ON w.applicant_dept_id = d.id
WHERE w.status = 1
ORDER BY w.id DESC
LIMIT 10;

-- 模拟深分页对比：记录相同筛选条件下的执行耗时
EXPLAIN SELECT ... ORDER BY w.id DESC LIMIT 5000, 10;
EXPLAIN SELECT ... WHERE w.id < 9000 ORDER BY w.id DESC LIMIT 10;
```

关注 `type`、`key`、`rows`、`Extra` 列：

- `type` 应为 `ref` / `range`，避免 `ALL`；
- `Extra` 不应出现 `Using filesort`（列表分页场景）；
- 游标分页的 `rows` 应始终接近页大小，不随页码增长。

## 4. 注意事项

- 数据量较小时优化器可能仍选择全表扫描，属正常现象；需在接近生产量的数据下评估。
- 关键字搜索使用前置通配的 `LIKE`，无法命中普通索引；数据量继续增长时可引入全文索引或搜索引擎。
- 统计接口建议加缓存或按需查询，避免高频调用对主表造成压力。
