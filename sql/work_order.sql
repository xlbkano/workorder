-- ============================================================
-- 内部工单审批管理系统 建库脚本（含可配置审批流）
-- MySQL 8.0+，默认账号 root/123456，可在 application.yml 中修改
-- ============================================================
CREATE DATABASE IF NOT EXISTS work_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE work_order;
SET NAMES utf8mb4;

DROP TABLE IF EXISTS operate_log;
DROP TABLE IF EXISTS work_order_task;
DROP TABLE IF EXISTS work_order_approval;
DROP TABLE IF EXISTS work_order;
DROP TABLE IF EXISTS approval_flow_node;
DROP TABLE IF EXISTS approval_flow;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS dept;

-- 部门表
CREATE TABLE dept (
    id          INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '部门id',
    name        VARCHAR(20)      NOT NULL UNIQUE COMMENT '部门名称',
    status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常',
    create_time DATETIME         NOT NULL COMMENT '创建时间',
    update_time DATETIME         NOT NULL COMMENT '修改时间'
) ENGINE = InnoDB COMMENT '部门表';

-- 用户表
CREATE TABLE `user` (
    id          INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '用户id',
    username    VARCHAR(20)      NOT NULL UNIQUE COMMENT '登录账号',
    password    VARCHAR(64)      NOT NULL COMMENT '密码（演示项目为明文，生产环境应加密）',
    name        VARCHAR(20)      NOT NULL COMMENT '姓名',
    role        VARCHAR(20)      NOT NULL COMMENT '角色：EMPLOYEE-员工，MANAGER-部门主管，ADMIN-管理员',
    dept_id     INT UNSIGNED     DEFAULT NULL COMMENT '部门id',
    status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常',
    create_time DATETIME         NOT NULL COMMENT '创建时间',
    update_time DATETIME         NOT NULL COMMENT '修改时间',
    KEY idx_user_dept_role (dept_id, role, status),
    KEY idx_user_role (role, status)
) ENGINE = InnoDB COMMENT '用户表';

-- 审批流程定义
CREATE TABLE approval_flow (
    id          INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '流程id',
    name        VARCHAR(30)      NOT NULL COMMENT '流程名称',
    code        VARCHAR(30)      NOT NULL COMMENT '流程编码',
    biz_type    TINYINT UNSIGNED DEFAULT NULL COMMENT '适用工单类型，NULL 表示通用流程',
    dept_id     INT UNSIGNED     DEFAULT NULL COMMENT '适用部门，NULL 表示不限部门',
    status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1-启用，0-停用',
    description VARCHAR(100)     DEFAULT NULL COMMENT '流程说明',
    create_time DATETIME         NOT NULL COMMENT '创建时间',
    update_time DATETIME         NOT NULL COMMENT '修改时间',
    UNIQUE KEY uk_flow_code (code),
    KEY idx_flow_match (status, biz_type, dept_id)
) ENGINE = InnoDB COMMENT '审批流程定义';

-- 审批流程节点
CREATE TABLE approval_flow_node (
    id            INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '节点id',
    flow_id       INT UNSIGNED     NOT NULL COMMENT '流程id',
    node_order    INT UNSIGNED     NOT NULL COMMENT '节点顺序，从 1 开始',
    node_name     VARCHAR(30)      NOT NULL COMMENT '节点名称',
    approver_type VARCHAR(20)      NOT NULL COMMENT 'DEPT_MANAGER/DEPT_ROLE/ROLE/USER/USER_LIST',
    approver_value VARCHAR(200)    DEFAULT NULL COMMENT '角色标识或用户id（多人逗号分隔）',
    multi_mode    VARCHAR(10)      NOT NULL DEFAULT 'OR' COMMENT 'OR-或签，AND-会签',
    allow_self    TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否允许申请人自己审批',
    optional_node TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '无人可用时是否自动跳过',
    create_time   DATETIME         NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_flow_node (flow_id, node_order)
) ENGINE = InnoDB COMMENT '审批流程节点';

-- 工单表
CREATE TABLE work_order (
    id                INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '工单id',
    order_no          VARCHAR(32)      NOT NULL COMMENT '工单号',
    title             VARCHAR(50)      NOT NULL COMMENT '标题',
    type              TINYINT UNSIGNED NOT NULL COMMENT '类型：1请假 2报销 3采购 4用印 5其他',
    priority          TINYINT UNSIGNED NOT NULL COMMENT '优先级：1低 2中 3高',
    content           VARCHAR(1000)    NOT NULL COMMENT '工单内容',
    applicant_id      INT UNSIGNED     NOT NULL COMMENT '申请人id',
    applicant_dept_id INT UNSIGNED     NOT NULL COMMENT '申请人部门id',
    approver_id       INT UNSIGNED     NOT NULL COMMENT '当前待处理审批人id',
    status            TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1待审批 2审批中 3已通过 4已驳回 5已撤销',
    flow_id           INT UNSIGNED     DEFAULT NULL COMMENT '审批流程id',
    current_node_order INT UNSIGNED    DEFAULT NULL COMMENT '当前审批节点顺序',
    round             INT UNSIGNED     NOT NULL DEFAULT 1 COMMENT '审批轮次',
    request_id        VARCHAR(64)      NOT NULL COMMENT '客户端幂等标识',
    version           INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time       DATETIME         NOT NULL COMMENT '创建时间',
    update_time       DATETIME         NOT NULL COMMENT '修改时间',
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_request_id (request_id),
    KEY idx_status_id (status, id),
    KEY idx_applicant_id (applicant_id, id),
    KEY idx_approver_id (approver_id, id),
    KEY idx_dept_id (applicant_dept_id, id),
    KEY idx_flow_id (flow_id)
) ENGINE = InnoDB COMMENT '工单表';

-- 审批任务表
CREATE TABLE work_order_task (
    id            INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '任务id',
    work_order_id INT UNSIGNED     NOT NULL COMMENT '工单id',
    flow_id       INT UNSIGNED     NOT NULL COMMENT '流程id',
    node_id       INT UNSIGNED     NOT NULL COMMENT '节点id',
    node_order    INT UNSIGNED     NOT NULL COMMENT '节点顺序',
    approver_id   INT UNSIGNED     NOT NULL COMMENT '审批人id',
    status        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0待处理 1已通过 2已驳回 3已转交 4已失效',
    round         INT UNSIGNED     NOT NULL DEFAULT 1 COMMENT '审批轮次',
    add_sign      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否加签任务',
    comment       VARCHAR(200)     DEFAULT NULL COMMENT '任务意见',
    create_time   DATETIME         NOT NULL COMMENT '创建时间',
    update_time   DATETIME         NOT NULL COMMENT '修改时间',
    UNIQUE KEY uk_task (work_order_id, round, node_id, approver_id),
    KEY idx_task_approver (approver_id, status, work_order_id),
    KEY idx_task_order (work_order_id, round, status)
) ENGINE = InnoDB COMMENT '审批任务表';

-- 审批记录表
CREATE TABLE work_order_approval (
    id            INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '记录id',
    work_order_id INT UNSIGNED     NOT NULL COMMENT '工单id',
    task_id       INT UNSIGNED     DEFAULT NULL COMMENT '审批任务id',
    node_id       INT UNSIGNED     DEFAULT NULL COMMENT '审批节点id',
    node_name     VARCHAR(30)      DEFAULT NULL COMMENT '节点名称快照',
    round         INT UNSIGNED     NOT NULL DEFAULT 1 COMMENT '审批轮次',
    approver_id   INT UNSIGNED     NOT NULL COMMENT '审批人id',
    action        TINYINT UNSIGNED NOT NULL COMMENT '动作：1通过 2驳回 3转交 4加签',
    comment       VARCHAR(200)     DEFAULT NULL COMMENT '审批意见',
    create_time   DATETIME         NOT NULL COMMENT '审批时间',
    KEY idx_approval_order (work_order_id, round, id),
    KEY idx_approval_user (approver_id, create_time)
) ENGINE = InnoDB COMMENT '审批记录表';

-- 操作日志表
CREATE TABLE operate_log (
    id              INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '日志id',
    operate_user_id INT UNSIGNED DEFAULT NULL COMMENT '操作人id',
    description     VARCHAR(50)  DEFAULT NULL COMMENT '操作说明',
    class_name      VARCHAR(100) DEFAULT NULL COMMENT '类名',
    method_name     VARCHAR(50)  DEFAULT NULL COMMENT '方法名',
    method_params   TEXT COMMENT '请求参数',
    return_value    TEXT COMMENT '返回结果 / 异常信息',
    cost_time       BIGINT UNSIGNED DEFAULT NULL COMMENT '耗时（毫秒）',
    operate_time    DATETIME     NOT NULL COMMENT '操作时间',
    KEY idx_log_user_time (operate_user_id, operate_time),
    KEY idx_log_time (operate_time)
) ENGINE = InnoDB COMMENT '操作日志表';

-- ============================================================
-- 初始化数据
-- ============================================================
INSERT INTO dept (id, name, status, create_time, update_time) VALUES
(1, '技术部', 1, NOW(), NOW()),
(2, '财务部', 1, NOW(), NOW()),
(3, '人事部', 1, NOW(), NOW());

-- 账号密码均为 123456
INSERT INTO `user` (id, username, password, name, role, dept_id, status, create_time, update_time) VALUES
(1, 'admin',    '123456', '系统管理员', 'ADMIN',    3, 1, NOW(), NOW()),
(2, 'zhangsan', '123456', '张三',       'EMPLOYEE', 1, 1, NOW(), NOW()),
(3, 'lisi',     '123456', '李四',       'MANAGER',  1, 1, NOW(), NOW()),
(4, 'wangwu',   '123456', '王五',       'EMPLOYEE', 2, 1, NOW(), NOW()),
(5, 'zhaoliu',  '123456', '赵六',       'MANAGER',  2, 1, NOW(), NOW()),
(6, 'sunqi',    '123456', '孙七',       'EMPLOYEE', 3, 1, NOW(), NOW());

-- 流程定义：通用 / 采购 / 报销
INSERT INTO approval_flow (id, name, code, biz_type, dept_id, status, description, create_time, update_time) VALUES
(1, '通用审批流程', 'GENERAL', NULL, NULL, 1, '兜底流程：部门主管 → 管理员', NOW(), NOW()),
(2, '采购审批流程', 'PURCHASE', 3, NULL, 1, '采购类：部门主管 → 财务负责人 → 管理员', NOW(), NOW()),
(3, '报销审批流程', 'EXPENSE', 2, NULL, 1, '报销类：部门主管 → 管理员', NOW(), NOW());

-- 流程节点
INSERT INTO approval_flow_node (id, flow_id, node_order, node_name, approver_type, approver_value, multi_mode, allow_self, optional_node, create_time) VALUES
(1, 1, 1, '部门主管审批', 'DEPT_MANAGER', NULL,  'OR', 0, 1, NOW()),
(2, 1, 2, '管理员审批',   'ROLE',         'ADMIN', 'OR', 0, 0, NOW()),
(3, 2, 1, '部门主管审批', 'DEPT_MANAGER', NULL,  'OR', 0, 0, NOW()),
(4, 2, 2, '财务负责人审核', 'USER',       '5',   'OR', 0, 0, NOW()),
(5, 2, 3, '管理员审批',   'ROLE',         'ADMIN', 'OR', 0, 0, NOW()),
(6, 3, 1, '部门主管审批', 'DEPT_MANAGER', NULL,  'OR', 0, 0, NOW()),
(7, 3, 2, '管理员审批',   'ROLE',         'ADMIN', 'OR', 0, 0, NOW());

-- 工单种子数据
INSERT INTO work_order (id, order_no, title, type, priority, content, applicant_id, applicant_dept_id, approver_id, status, flow_id, current_node_order, round, request_id, version, create_time, update_time) VALUES
(1, 'WO202611010001', '年假申请',       1, 2, '申请下周一至周三休年假三天，工作已交接。', 2, 1, 1, 3, 1, 2, 1, 'seed-0001', 1, '2026-11-01 09:10:00', '2026-11-01 15:20:00'),
(2, 'WO202611020002', '差旅报销',       2, 2, '10 月北京出差交通与住宿费用，合计 2860 元。', 2, 1, 3, 1, 3, 1, 1, 'seed-0002', 0, '2026-11-02 10:30:00', '2026-11-02 10:30:00'),
(3, 'WO202611030003', '采购显示器 2 台', 3, 3, '研发工位新增两台 27 寸显示器，预算 3000 元。', 4, 2, 5, 1, 2, 1, 1, 'seed-0003', 0, '2026-11-03 11:05:00', '2026-11-03 11:05:00'),
(4, 'WO202611040004', '合同用印申请',   4, 3, '客户合同需加盖公司公章，请审批。', 4, 2, 5, 4, 1, 1, 1, 'seed-0004', 1, '2026-11-04 14:00:00', '2026-11-04 16:40:00'),
(5, 'WO202611050005', '办公用品领用',   5, 1, '申请领用打印纸与签字笔若干。', 6, 3, 1, 1, 1, 2, 1, 'seed-0005', 0, '2026-11-05 09:20:00', '2026-11-05 09:20:00'),
(6, 'WO202611060006', '采购会议白板',   3, 2, '会议室新增可移动白板一块，预算 600 元。', 2, 1, 3, 1, 2, 1, 1, 'seed-0006', 0, '2026-11-06 13:15:00', '2026-11-06 13:15:00'),
(7, 'WO202611070007', '调休申请',       1, 1, '周末加班调休一天，申请周二调休。', 2, 1, 3, 5, 1, 1, 1, 'seed-0007', 1, '2026-11-07 08:40:00', '2026-11-07 09:05:00'),
(8, 'WO202611080008', '培训费用报销',   2, 2, '参加行业培训费用 1200 元，附电子发票。', 4, 2, 1, 3, 3, 2, 1, 'seed-0008', 2, '2026-11-08 10:00:00', '2026-11-09 11:30:00');

-- 审批任务种子数据
INSERT INTO work_order_task (id, work_order_id, flow_id, node_id, node_order, approver_id, status, round, add_sign, comment, create_time, update_time) VALUES
(1, 1, 1, 1, 1, 3, 1, 1, 0, '同意，注意工作交接。', '2026-11-01 09:10:00', '2026-11-01 11:00:00'),
(2, 1, 1, 2, 2, 1, 1, 1, 0, '批准。',             '2026-11-01 11:00:00', '2026-11-01 15:20:00'),
(3, 2, 3, 6, 1, 3, 0, 1, 0, NULL,                 '2026-11-02 10:30:00', '2026-11-02 10:30:00'),
(4, 3, 2, 3, 1, 5, 0, 1, 0, NULL,                 '2026-11-03 11:05:00', '2026-11-03 11:05:00'),
(5, 4, 1, 1, 1, 5, 2, 1, 0, '合同条款需法务复核后再提交。', '2026-11-04 14:00:00', '2026-11-04 16:40:00'),
(6, 5, 1, 2, 2, 1, 0, 1, 0, NULL,                 '2026-11-05 09:20:00', '2026-11-05 09:20:00'),
(7, 6, 2, 3, 1, 3, 0, 1, 0, NULL,                 '2026-11-06 13:15:00', '2026-11-06 13:15:00'),
(8, 7, 1, 1, 1, 3, 4, 1, 0, NULL,                 '2026-11-07 08:40:00', '2026-11-07 09:05:00'),
(9, 8, 3, 6, 1, 5, 1, 1, 0, '同意报销。',          '2026-11-08 10:00:00', '2026-11-08 15:00:00'),
(10, 8, 3, 7, 2, 1, 1, 1, 0, '费用合规，批准。',    '2026-11-09 09:00:00', '2026-11-09 11:30:00');

-- 审批记录
INSERT INTO work_order_approval (id, work_order_id, task_id, node_id, node_name, round, approver_id, action, comment, create_time) VALUES
(1, 1, 1, 1, '部门主管审批', 1, 3, 1, '同意，注意工作交接。', '2026-11-01 11:00:00'),
(2, 1, 2, 2, '管理员审批',   1, 1, 1, '批准。',             '2026-11-01 15:20:00'),
(3, 4, 5, 1, '部门主管审批', 1, 5, 2, '合同条款需法务复核后再提交。', '2026-11-04 16:40:00'),
(4, 8, 9, 6, '部门主管审批', 1, 5, 1, '同意报销。',          '2026-11-08 15:00:00'),
(5, 8, 10, 7, '管理员审批',  1, 1, 1, '费用合规，批准。',     '2026-11-09 11:30:00');

INSERT INTO operate_log (operate_user_id, description, class_name, method_name, method_params, return_value, cost_time, operate_time) VALUES
(2, '提交工单', 'com.workorder.controller.WorkOrderController', 'add', '示例参数', '工单提交成功', 35, '2026-11-01 09:10:00'),
(3, '审批工单', 'com.workorder.controller.WorkOrderController', 'approve', '示例参数', '审批完成', 28, '2026-11-01 11:00:00');
