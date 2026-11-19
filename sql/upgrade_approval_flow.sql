-- ============================================================
-- 存量库升级脚本：增加可配置审批流
-- 适用：已按旧版 work_order.sql 建库且存在业务数据的环境
-- 执行前请先备份数据库；重复执行前请确认 ALTER 字段尚未添加
-- ============================================================
USE work_order;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS approval_flow (
    id          INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(30)      NOT NULL,
    code        VARCHAR(30)      NOT NULL,
    biz_type    TINYINT UNSIGNED DEFAULT NULL,
    dept_id     INT UNSIGNED     DEFAULT NULL,
    status      TINYINT UNSIGNED NOT NULL DEFAULT 1,
    description VARCHAR(100)     DEFAULT NULL,
    create_time DATETIME         NOT NULL,
    update_time DATETIME         NOT NULL,
    UNIQUE KEY uk_flow_code (code),
    KEY idx_flow_match (status, biz_type, dept_id)
) ENGINE = InnoDB COMMENT '审批流程定义';

CREATE TABLE IF NOT EXISTS approval_flow_node (
    id            INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    flow_id       INT UNSIGNED     NOT NULL,
    node_order    INT UNSIGNED     NOT NULL,
    node_name     VARCHAR(30)      NOT NULL,
    approver_type VARCHAR(20)      NOT NULL,
    approver_value VARCHAR(200)    DEFAULT NULL,
    multi_mode    VARCHAR(10)      NOT NULL DEFAULT 'OR',
    allow_self    TINYINT UNSIGNED NOT NULL DEFAULT 0,
    optional_node TINYINT UNSIGNED NOT NULL DEFAULT 0,
    create_time   DATETIME         NOT NULL,
    UNIQUE KEY uk_flow_node (flow_id, node_order)
) ENGINE = InnoDB COMMENT '审批流程节点';

CREATE TABLE IF NOT EXISTS work_order_task (
    id            INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    work_order_id INT UNSIGNED     NOT NULL,
    flow_id       INT UNSIGNED     NOT NULL,
    node_id       INT UNSIGNED     NOT NULL,
    node_order    INT UNSIGNED     NOT NULL,
    approver_id   INT UNSIGNED     NOT NULL,
    status        TINYINT UNSIGNED NOT NULL DEFAULT 0,
    round         INT UNSIGNED     NOT NULL DEFAULT 1,
    add_sign      TINYINT UNSIGNED NOT NULL DEFAULT 0,
    comment       VARCHAR(200)     DEFAULT NULL,
    create_time   DATETIME         NOT NULL,
    update_time   DATETIME         NOT NULL,
    UNIQUE KEY uk_task (work_order_id, round, node_id, approver_id),
    KEY idx_task_approver (approver_id, status, work_order_id),
    KEY idx_task_order (work_order_id, round, status)
) ENGINE = InnoDB COMMENT '审批任务表';

ALTER TABLE work_order
    ADD COLUMN flow_id INT UNSIGNED DEFAULT NULL COMMENT '审批流程id',
    ADD COLUMN current_node_order INT UNSIGNED DEFAULT NULL COMMENT '当前审批节点顺序',
    ADD COLUMN round INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '审批轮次',
    ADD KEY idx_flow_id (flow_id);

ALTER TABLE work_order_approval
    ADD COLUMN task_id INT UNSIGNED DEFAULT NULL COMMENT '审批任务id',
    ADD COLUMN node_id INT UNSIGNED DEFAULT NULL COMMENT '审批节点id',
    ADD COLUMN node_name VARCHAR(30) DEFAULT NULL COMMENT '节点名称快照',
    ADD COLUMN round INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '审批轮次';

-- 默认通用流程：部门主管 → 管理员
INSERT IGNORE INTO approval_flow (id, name, code, biz_type, dept_id, status, description, create_time, update_time)
VALUES (1, '通用审批流程', 'GENERAL', NULL, NULL, 1, '兜底流程：部门主管 → 管理员', NOW(), NOW());

INSERT IGNORE INTO approval_flow_node (id, flow_id, node_order, node_name, approver_type, approver_value, multi_mode, allow_self, optional_node, create_time) VALUES
(1, 1, 1, '部门主管审批', 'DEPT_MANAGER', NULL,  'OR', 0, 1, NOW()),
(2, 1, 2, '管理员审批',   'ROLE',         'ADMIN', 'OR', 0, 0, NOW());

-- 存量未结束工单回填流程与第一节点任务
UPDATE work_order
SET flow_id = 1, current_node_order = 1, round = 1
WHERE flow_id IS NULL;

INSERT INTO work_order_task (work_order_id, flow_id, node_id, node_order, approver_id, status, round, add_sign, create_time, update_time)
SELECT w.id, 1, 1, 1, w.approver_id, 0, 1, 0, NOW(), NOW()
FROM work_order w
WHERE w.status IN (1, 2)
  AND NOT EXISTS (SELECT 1 FROM work_order_task t WHERE t.work_order_id = w.id);
