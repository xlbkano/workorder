package com.workorder;

import com.workorder.common.WorkOrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工单状态机：验证各状态允许的动作，避免非法迁移
 */
class WorkOrderStatusTest {

    @Test
    void activeStatusesAllowApproveAndWithdraw() {
        for (WorkOrderStatus status : new WorkOrderStatus[]{
                WorkOrderStatus.PENDING, WorkOrderStatus.PROCESSING}) {
            assertTrue(status.isActive(), status + " 应为进行中");
            assertTrue(status.canApprove(), status + " 应允许审批");
            assertTrue(status.canWithdraw(), status + " 应允许撤销");
            assertFalse(status.canResubmit(), status + " 不应允许重新提交");
        }
    }

    @Test
    void rejectedStatusOnlyAllowsResubmit() {
        WorkOrderStatus status = WorkOrderStatus.REJECTED;
        assertFalse(status.isActive());
        assertFalse(status.canApprove());
        assertFalse(status.canWithdraw());
        assertTrue(status.canResubmit());
    }

    @Test
    void terminalStatusesAllowNoAction() {
        for (WorkOrderStatus status : new WorkOrderStatus[]{
                WorkOrderStatus.APPROVED, WorkOrderStatus.WITHDRAWN}) {
            assertFalse(status.isActive());
            assertFalse(status.canApprove());
            assertFalse(status.canWithdraw());
            assertFalse(status.canResubmit());
        }
    }

    @Test
    void codeToStatusMapping() {
        assertEquals(WorkOrderStatus.PENDING, WorkOrderStatus.of(1));
        assertEquals(WorkOrderStatus.REJECTED, WorkOrderStatus.of(4));
        assertNull(WorkOrderStatus.of(null));
        assertNull(WorkOrderStatus.of(99));
        assertEquals("已通过", WorkOrderStatus.APPROVED.getLabel());
    }
}
