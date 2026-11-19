package com.workorder.common;

/**
 * 工单状态机：统一维护状态含义与可用动作，避免散落在业务代码中的魔法值判断
 */
public enum WorkOrderStatus {

    PENDING(1, "待审批"),
    PROCESSING(2, "审批中"),
    APPROVED(3, "已通过"),
    REJECTED(4, "已驳回"),
    WITHDRAWN(5, "已撤销");

    private final int code;
    private final String label;

    WorkOrderStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static WorkOrderStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (WorkOrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    /** 流程是否仍在进行中 */
    public boolean isActive() {
        return this == PENDING || this == PROCESSING;
    }

    /** 是否可以审批 */
    public boolean canApprove() {
        return isActive();
    }

    /** 是否可以撤销 */
    public boolean canWithdraw() {
        return isActive();
    }

    /** 是否可以重新提交 */
    public boolean canResubmit() {
        return this == REJECTED;
    }
}
