package com.workorder.domain.vo;

import com.workorder.domain.pojo.WorkOrder;
import com.workorder.domain.pojo.WorkOrderApproval;
import com.workorder.domain.pojo.WorkOrderTask;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkOrderDetailVo {

    private WorkOrder order;
    private List<WorkOrderApproval> approvals;
    /** 当前轮次的审批任务 */
    private List<WorkOrderTask> tasks;
    private String flowName;
    private String currentNodeName;
    /** 当前登录人是否可以审批 */
    private Boolean canApprove;
    /** 当前登录人是否可以转交 */
    private Boolean canTransfer;
    /** 当前登录人是否可以加签 */
    private Boolean canAddSign;
    /** 当前登录人是否可以撤销 */
    private Boolean canWithdraw;
    /** 当前登录人是否可以重新提交 */
    private Boolean canResubmit;
}
