package com.workorder.common;

/**
 * 工单业务常量：角色、状态、审批动作、类型、优先级
 */
public final class WorkOrderConst {

    private WorkOrderConst() {
    }

    /* ================= 角色 ================= */
    public static final String ROLE_EMPLOYEE = "EMPLOYEE";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_ADMIN = "ADMIN";

    /* ================= 工单状态 ================= */
    public static final int STATUS_PENDING = 1;      // 待审批
    public static final int STATUS_PROCESSING = 2;   // 审批中
    public static final int STATUS_APPROVED = 3;     // 已通过
    public static final int STATUS_REJECTED = 4;     // 已驳回
    public static final int STATUS_WITHDRAWN = 5;    // 已撤销

    /* ================= 审批动作 ================= */
    public static final int ACTION_APPROVE = 1;      // 通过
    public static final int ACTION_REJECT = 2;       // 驳回
    public static final int ACTION_TRANSFER = 3;     // 转交
    public static final int ACTION_ADD_SIGN = 4;     // 加签

    /* ================= 审批任务状态 ================= */
    public static final int TASK_PENDING = 0;        // 待处理
    public static final int TASK_APPROVED = 1;       // 已通过
    public static final int TASK_REJECTED = 2;       // 已驳回
    public static final int TASK_TRANSFERRED = 3;    // 已转交
    public static final int TASK_INVALID = 4;        // 已失效

    /* ================= 审批人类型 ================= */
    public static final String APPROVER_DEPT_MANAGER = "DEPT_MANAGER"; // 申请人部门主管
    public static final String APPROVER_DEPT_ROLE = "DEPT_ROLE";       // 申请人部门内指定角色
    public static final String APPROVER_ROLE = "ROLE";                 // 全局指定角色
    public static final String APPROVER_USER = "USER";                 // 指定用户
    public static final String APPROVER_USER_LIST = "USER_LIST";       // 指定多人

    /* ================= 节点签核方式 ================= */
    public static final String NODE_MODE_OR = "OR";   // 或签：一人通过即推进
    public static final String NODE_MODE_AND = "AND"; // 会签：全部通过才推进

    /* ================= 用户 / 流程状态 ================= */
    public static final int USER_ENABLED = 1;
    public static final int FLOW_ENABLED = 1;

    public static String actionName(Integer action) {
        if (action == null) {
            return "未知";
        }
        return switch (action) {
            case ACTION_APPROVE -> "通过";
            case ACTION_REJECT -> "驳回";
            case ACTION_TRANSFER -> "转交";
            case ACTION_ADD_SIGN -> "加签";
            default -> "未知";
        };
    }

    public static String statusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case STATUS_PENDING -> "待审批";
            case STATUS_PROCESSING -> "审批中";
            case STATUS_APPROVED -> "已通过";
            case STATUS_REJECTED -> "已驳回";
            case STATUS_WITHDRAWN -> "已撤销";
            default -> "未知";
        };
    }

    public static String typeName(Integer type) {
        if (type == null) {
            return "未知";
        }
        return switch (type) {
            case 1 -> "请假";
            case 2 -> "报销";
            case 3 -> "采购";
            case 4 -> "用印";
            case 5 -> "其他";
            default -> "未知";
        };
    }

    public static String priorityName(Integer priority) {
        if (priority == null) {
            return "未知";
        }
        return switch (priority) {
            case 1 -> "低";
            case 2 -> "中";
            case 3 -> "高";
            default -> "未知";
        };
    }
}
