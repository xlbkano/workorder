# 内部工单审批管理系统

面向企业内部审批场景：员工提交工单、多级流转审核，记录审批记录与操作日志，保障流程可追溯。
审批流程支持后台配置：按工单类型/部门匹配，支持或签、会签、转交、加签、驳回后重新提交。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.4.8 / Java 21 / Maven 多模块 |
| 持久层 | MyBatis-Plus 3.5.17 + MyBatis XML（多表联查、分页、条件统计） |
| 数据库 | MySQL 8.0 |
| 鉴权 | JWT（登录签发，拦截器校验） |
| 接口文档 | springdoc-openapi（Swagger UI） |
| 前端 | 原生 HTML + CSS + JS（nginx 托管，无需构建） |
| 部署 | nginx 静态页 + `/api` 反向代理 |

## 功能特性

- **可配置审批流**：流程与节点存库，管理员可在页面新建/编辑/启停；按类型 + 部门匹配，支持兜底流程
- **多级流转**：顺序节点、或签、会签、条件跳过（无人可审时）
- **审批动作**：通过、驳回、转交、加签、撤销、驳回后重新提交（轮次 +1，历史保留）
- **状态机**：统一状态枚举与迁移校验，非法操作返回标准提示
- **并发安全**：工单行锁 + 任务条件更新 + 乐观锁，避免重复审批
- **可追溯**：审批记录保存节点与轮次快照，操作日志记录提交/审批/转交/加签/撤销
- **参数校验与全局异常**、统一返回、幂等提交、Swagger 文档

## 目录结构

```
work-order-parent
├─ pom.xml
├─ work-order-common        # Result、PageData、JwtUtil、UserContext、状态机与业务常量
├─ work-order-entities      # pojo / dto / vo
├─ work-order-management    # 启动类、controller、service、mapper、config、exception、aspect
│   └─ src/main/resources   # application.yml、mapper/*.xml
├─ sql/
│   ├─ work_order.sql                 # 完整建库脚本（含流程种子数据）
│   └─ upgrade_approval_flow.sql      # 存量库升级脚本
├─ web/                     # 前端源码（登录页、工作台、流程配置）
├─ deploy/nginx.conf        # nginx 配置示例（90 端口静态页 + /api 反代）
├─ docs/                    # 审批流设计、边界用例、自测清单、SQL 优化说明
└─ README.md

E:\javaWork\work-order-nginx  # 独立 nginx 目录（新前端页面 + 反向代理配置）
```

## 快速开始

### 1. 初始化数据库

```bash
mysql --default-character-set=utf8mb4 -uroot -p123456 < sql/work_order.sql
```

存量库升级：

```bash
mysql --default-character-set=utf8mb4 -uroot -p123456 < sql/upgrade_approval_flow.sql
```

### 2. 启动后端（8080）

```bash
mvn clean package -DskipTests
java -jar work-order-management/target/work-order-management-1.0-SNAPSHOT.jar
```

或直接在 IDE 中运行 `com.workorder.App`。

- 接口文档：http://localhost:8080/swagger-ui.html
- 数据库连接在 `work-order-management/src/main/resources/application.yml` 中修改

### 3. 启动前端（nginx，90 端口）

```bash
cd E:\javaWork\work-order-nginx
start nginx.exe
```

访问 http://localhost:90

> 如旧 nginx 正在占用 90 端口，先停止旧进程：`nginx.exe -s stop`。
>
> 仓库内 `web/` 为前端源码，`deploy/nginx.conf` 为配置示例；本地可直接使用 `E:\javaWork\work-order-nginx`，或将 `web/` 内容放入 nginx 的 `html/` 目录并替换配置。

### 演示账号（密码均为 `123456`）

| 账号 | 角色 | 部门 |
|---|---|---|
| admin | 管理员 | 人事部 |
| lisi | 部门主管 | 技术部 |
| zhaoliu | 部门主管 | 财务部 |
| zhangsan | 员工 | 技术部 |
| wangwu | 员工 | 财务部 |
| sunqi | 员工 | 人事部 |

## 审批流程配置

管理员登录后进入「流程配置」：

- 新建流程：填写名称、编码、适用类型/部门，按顺序添加节点
- 节点审批人：部门主管、部门内角色、全局角色、指定用户、指定多人
- 签核方式：或签（一人通过）/ 会签（全部通过）
- 可设置“允许申请人自己审批”“无人可审时自动跳过”
- 存在审批中工单的流程不允许修改，避免影响在途流程

种子流程：

| 流程 | 适用类型 | 节点 |
|---|---|---|
| 通用审批流程 | 通用 | 部门主管 → 管理员 |
| 采购审批流程 | 采购 | 部门主管 → 财务负责人 → 管理员 |
| 报销审批流程 | 报销 | 部门主管 → 管理员 |

## 核心接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/login` | 登录，返回 token |
| GET | `/work-orders` | 工单分页列表（状态/类型/关键字筛选，支持 `lastId` 游标分页） |
| GET | `/work-orders/todo` | 待我审批（基于审批任务） |
| GET | `/work-orders/{id}` | 详情 + 当前轮次任务 + 审批记录 + 可执行动作 |
| POST | `/work-orders` | 提交工单（需 `X-Request-Id`，幂等控制） |
| POST | `/work-orders/{id}/approve` | 审批：`action=1` 通过 / `2` 驳回 |
| POST | `/work-orders/{id}/transfer` | 转交 `{toUserId}` |
| POST | `/work-orders/{id}/add-sign` | 加签 `{userIds:[...]}` |
| POST | `/work-orders/{id}/resubmit` | 驳回后重新提交 |
| PUT | `/work-orders/{id}/withdraw` | 撤销工单 |
| GET | `/work-orders/stats` | 状态 / 类型 / 部门统计 |
| GET | `/flows` | 流程配置列表（管理员） |
| POST | `/flows` | 新建/编辑流程与节点（管理员） |
| PUT | `/flows/{id}/status` | 启用/停用流程（管理员） |
| GET | `/users`、`/depts` | 用户、部门列表 |
| GET | `/logs` | 操作日志（仅管理员） |

除 `/login` 与 Swagger 相关路径外，请求头需携带 `token`。

## 设计要点

1. **参数校验与全局异常**：DTO 使用 Bean Validation；`GlobalException` 统一处理参数绑定、JSON 解析、约束校验、唯一键冲突、业务异常与兜底异常。40 条边界用例中 34 条非法请求被拦截（见 `docs/test-cases.md`，可用 `mvn test` 复跑）。
2. **可配置审批流**：`approval_flow` + `approval_flow_node` 定义流程，`work_order_task` 承载运行态任务，`ApprovalEngine` 负责节点解析与推进；详见 `docs/approval-flow.md`。
3. **查询优化**：列表 SQL 不查 `content` 大字段；多表联查申请人/部门/审批人/当前节点；联合索引覆盖状态、申请人、审批人、部门维度；支持 `lastId` 游标分页（见 `docs/sql-optimization.md`）。
4. **统一返回与幂等**：`Result{code,msg,data,timestamp}`；`@Idempotent` 切面按“用户 + X-Request-Id”在 5 分钟窗口内去重，`work_order.request_id` 唯一索引兜底多实例场景。
5. **流程可追溯**：每次审批写入 `work_order_approval`（含节点、轮次、动作）；提交、审批、转交、加签、撤销通过 `@LogRecord` 切面记录到 `operate_log`，日志失败不影响主流程。
6. **并发安全**：审批动作使用 `SELECT ... FOR UPDATE` 串行化同一工单的审批；任务条件更新 + 工单乐观锁，重复审批只有一次成功。

## 文档

- [可配置审批流设计](docs/approval-flow.md)
- [40 条边界用例](docs/test-cases.md)
- [开发自测清单](docs/self-test.md)
- [SQL 与索引优化说明](docs/sql-optimization.md)
