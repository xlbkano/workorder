# 开发自测清单

每次提交前按顺序自测，接口地址以 `http://localhost:8080` 为例（经 nginx 时加 `/api` 前缀）。

## 1. 登录与鉴权

- [ ] 正确账号密码登录成功，返回 token
- [ ] 密码错误 → “用户名或密码错误”
- [ ] 不传 token 访问 `/work-orders` → HTTP 401 + “登录已失效”
- [ ] 篡改 token → HTTP 401

```bash
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"123456"}'
```

## 2. 提交工单与幂等

- [ ] 正常提交成功，状态为“待审批”，审批人为同部门主管
- [ ] 同一 `X-Request-Id` 重复提交 → “请勿重复提交”
- [ ] 缺少 `X-Request-Id` → “缺少幂等标识”
- [ ] 标题/内容/类型/优先级非法 → 400 + 对应提示
- [ ] 参数 JSON 格式错误 → 400 + “请求参数格式错误”

```bash
curl -X POST http://localhost:8080/work-orders \
  -H "Content-Type: application/json" \
  -H "token: <token>" \
  -H "X-Request-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"title":"采购键盘","type":3,"priority":2,"content":"采购机械键盘两把用于研发"}'
```

## 3. 审批流转

- [ ] 按工单类型匹配到正确流程（通用/采购/报销）
- [ ] 主管审批通过 → 状态“审批中”，审批人变为下一节点审批人
- [ ] 管理员终审通过 → 状态“已通过”
- [ ] 或签节点：一人通过后其余任务失效，进入下一节点
- [ ] 会签节点：全部通过才推进；任一驳回则工单驳回
- [ ] 加签后节点按会签处理，追加人通过后才推进
- [ ] 转交后原审批人待办消失，新审批人可处理
- [ ] 驳回不填意见 → 拦截
- [ ] 驳回填写意见 → 状态“已驳回”，记录可见
- [ ] 驳回后申请人重新提交 → 轮次 +1，历史记录保留
- [ ] 非当前审批人操作 → “您没有待处理的审批任务”
- [ ] 已结束工单再次审批 → “当前工单状态不允许审批”
- [ ] 两人同时审批同一工单 → 仅一次成功，另一次提示刷新

```bash
curl -X POST http://localhost:8080/work-orders/2/approve \
  -H "Content-Type: application/json" \
  -H "token: <主管token>" \
  -d '{"action":1,"comment":"同意"}'

curl -X POST http://localhost:8080/work-orders/2/transfer \
  -H "Content-Type: application/json" \
  -H "token: <当前审批人token>" \
  -d '{"toUserId":1}'

curl -X POST http://localhost:8080/work-orders/2/add-sign \
  -H "Content-Type: application/json" \
  -H "token: <当前审批人token>" \
  -d '{"userIds":[1,5]}'

curl -X POST http://localhost:8080/work-orders/2/resubmit \
  -H "token: <申请人token>"
```

## 3.1 流程配置（管理员）

- [ ] 流程列表展示节点链路与启停状态
- [ ] 新建流程并配置 2~3 个节点，保存成功
- [ ] 指定用户/角色/多人的节点配置正确回显
- [ ] 同编码或同适用范围重复启用 → 拦截
- [ ] 存在审批中工单的流程编辑 → “暂不能修改”
- [ ] 停用流程后提交同类工单 → 匹配到其他可用流程
- [ ] 非管理员访问 `/flows` → 无权限提示

## 4. 撤销

- [ ] 申请人撤销待审批工单成功
- [ ] 非申请人撤销 → 拦截
- [ ] 已通过/已驳回工单撤销 → 拦截

## 5. 查询与统计

- [ ] 员工只见自己的工单；主管见本部门；管理员见全部
- [ ] 状态、类型、关键字筛选结果正确
- [ ] 分页参数越界（page=0、size=101）→ 400
- [ ] `lastId` 游标分页第二页数据正确、无重复
- [ ] 统计卡片合计与列表总数一致
- [ ] 查看他人无权工单 → “无权查看该工单”

## 6. 操作日志

- [ ] 提交/审批/撤销后 `operate_log` 有记录，操作人、参数、耗时正确
- [ ] 管理员查看 `/logs` 正常
- [ ] 非管理员查看 `/logs` → 无权限

## 7. 前端联动

- [ ] 登录后跳转工作台，刷新页面保持登录
- [ ] 工单列表、筛选、分页、滚动加载正常
- [ ] 新建工单成功后列表刷新
- [ ] 详情页审批时间线顺序正确
- [ ] 重复点击“提交”只产生一条工单
- [ ] token 过期后自动跳回登录页
