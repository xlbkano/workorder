package com.workorder.service;

import cn.hutool.core.util.StrUtil;
import com.workorder.common.UserContext;
import com.workorder.common.WorkOrderConst;
import com.workorder.common.WorkOrderStatus;
import com.workorder.domain.dto.ApproveDto;
import com.workorder.domain.pojo.ApprovalFlow;
import com.workorder.domain.pojo.ApprovalFlowNode;
import com.workorder.domain.pojo.User;
import com.workorder.domain.pojo.WorkOrder;
import com.workorder.domain.pojo.WorkOrderApproval;
import com.workorder.domain.pojo.WorkOrderTask;
import com.workorder.exception.CustomException;
import com.workorder.mapper.UserMapper;
import com.workorder.mapper.WorkOrderApprovalMapper;
import com.workorder.mapper.WorkOrderMapper;
import com.workorder.mapper.WorkOrderTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 审批流转引擎：
 * 负责节点解析、任务生成、或签/会签推进、转交、加签、重新提交与状态迁移。
 */
@Service
public class ApprovalEngine {

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private WorkOrderTaskMapper taskMapper;

    @Autowired
    private WorkOrderApprovalMapper approvalMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ApprovalFlowService flowService;

    @Autowired
    private UserContext userContext;

    /** 起始节点及解析出的审批人 */
    public record StartNode(ApprovalFlowNode node, List<User> approvers) {
    }

    /**
     * 从流程第一个节点开始，跳过“可选且无人可用”的节点，返回首个有效节点
     */
    public StartNode resolveStart(ApprovalFlow flow, User applicant) {
        List<ApprovalFlowNode> nodes = flow.getNodes() == null
                ? flowService.nodesOf(flow.getId()) : flow.getNodes();
        for (ApprovalFlowNode node : nodes) {
            List<User> approvers = flowService.resolveApprovers(node, applicant);
            if (!approvers.isEmpty()) {
                return new StartNode(node, approvers);
            }
        }
        throw new CustomException("审批流程未配置可用审批人");
    }

    public void createTasks(WorkOrder order, ApprovalFlowNode node, List<User> approvers) {
        for (User approver : approvers) {
            WorkOrderTask task = new WorkOrderTask();
            task.setWorkOrderId(order.getId());
            task.setFlowId(order.getFlowId());
            task.setNodeId(node.getId());
            task.setNodeOrder(node.getNodeOrder());
            task.setApproverId(approver.getId());
            task.setStatus(WorkOrderConst.TASK_PENDING);
            task.setRound(order.getRound());
            task.setAddSign(0);
            taskMapper.insert(task);
        }
    }

    /**
     * 当前审批人处理任务；通过后按或签/会签规则推进，驳回则终止流程
     */
    public void approve(WorkOrder order, ApproveDto dto) {
        Integer userId = requireUserId();
        WorkOrderTask task = taskMapper.findUserPendingTask(order.getId(), userId, order.getRound());
        if (task == null) {
            throw new CustomException("您没有待处理的审批任务");
        }
        if (!Objects.equals(task.getNodeOrder(), order.getCurrentNodeOrder())) {
            throw new CustomException("当前审批节点已变更，请刷新页面后重试");
        }
        ApprovalFlowNode node = flowService.requireNode(task.getNodeId());
        String comment = StrUtil.trimToEmpty(dto.getComment());

        if (dto.getAction() == WorkOrderConst.ACTION_REJECT) {
            if (StrUtil.isBlank(comment)) {
                throw new CustomException("驳回时必须填写审批意见");
            }
            int rows = taskMapper.updateTaskStatus(task.getId(), WorkOrderConst.TASK_REJECTED, comment);
            if (rows == 0) {
                throw new CustomException("该审批任务已被处理，请刷新页面后重试");
            }
            taskMapper.invalidatePending(order.getId(), order.getRound(), node.getId(), task.getId());
            order.setStatus(WorkOrderConst.STATUS_REJECTED);
            workOrderMapper.updateById(order);
            record(order, task, node, WorkOrderConst.ACTION_REJECT, comment);
            return;
        }

        String approveComment = StrUtil.blankToDefault(comment, "同意");
        int rows = taskMapper.updateTaskStatus(task.getId(), WorkOrderConst.TASK_APPROVED, approveComment);
        if (rows == 0) {
            throw new CustomException("该审批任务已被处理，请刷新页面后重试");
        }
        record(order, task, node, WorkOrderConst.ACTION_APPROVE, approveComment);
        advance(order, node);
    }

    private void advance(WorkOrder order, ApprovalFlowNode node) {
        long pending = taskMapper.countPendingByNode(order.getId(), order.getRound(), node.getId());
        long addSignCount = taskMapper.countAddSignByNode(order.getId(), order.getRound(), node.getId());
        boolean andMode = WorkOrderConst.NODE_MODE_AND.equals(node.getMultiMode()) || addSignCount > 0;

        if (andMode && pending > 0) {
            // 会签未完成，等待其余审批人
            order.setStatus(WorkOrderConst.STATUS_PROCESSING);
            order.setApproverId(firstPendingApprover(order, node));
            workOrderMapper.updateById(order);
            return;
        }

        // 或签：其余任务失效；会签：理论上已无待处理任务
        taskMapper.invalidatePending(order.getId(), order.getRound(), node.getId(), null);
        goNextNode(order, node.getNodeOrder());
    }

    private void goNextNode(WorkOrder order, int currentNodeOrder) {
        ApprovalFlow flow = flowService.requireFlowById(order.getFlowId());
        User applicant = userMapper.selectById(order.getApplicantId());
        if (applicant == null) {
            throw new CustomException("申请人信息异常，无法继续流转");
        }

        for (ApprovalFlowNode node : flow.getNodes()) {
            if (node.getNodeOrder() <= currentNodeOrder) {
                continue;
            }
            List<User> approvers = flowService.resolveApprovers(node, applicant);
            if (approvers.isEmpty()) {
                // 可选节点无人可用，自动跳过
                continue;
            }
            createTasks(order, node, approvers);
            order.setCurrentNodeOrder(node.getNodeOrder());
            order.setStatus(WorkOrderConst.STATUS_PROCESSING);
            order.setApproverId(approvers.get(0).getId());
            workOrderMapper.updateById(order);
            return;
        }

        order.setStatus(WorkOrderConst.STATUS_APPROVED);
        workOrderMapper.updateById(order);
    }

    public void withdraw(WorkOrder order) {
        taskMapper.invalidatePending(order.getId(), order.getRound(), null, null);
        order.setStatus(WorkOrderConst.STATUS_WITHDRAWN);
        workOrderMapper.updateById(order);
    }

    /**
     * 驳回后重新提交：轮次 +1，按当前最新流程配置重新生成任务，历史审批记录保留
     */
    public void resubmit(WorkOrder order, User applicant) {
        ApprovalFlow flow = flowService.requireFlow(order.getType(), order.getApplicantDeptId());
        StartNode start = resolveStart(flow, applicant);

        int oldRound = order.getRound() == null ? 1 : order.getRound();
        taskMapper.invalidatePending(order.getId(), oldRound, null, null);

        order.setFlowId(flow.getId());
        order.setRound(oldRound + 1);
        order.setCurrentNodeOrder(start.node().getNodeOrder());
        order.setApproverId(start.approvers().get(0).getId());
        order.setStatus(WorkOrderConst.STATUS_PENDING);
        workOrderMapper.updateById(order);

        createTasks(order, start.node(), start.approvers());
    }

    public void transfer(WorkOrder order, Integer toUserId) {
        Integer userId = requireUserId();
        if (userId.equals(toUserId)) {
            throw new CustomException("不能转交给自己");
        }
        WorkOrderTask task = taskMapper.findUserPendingTask(order.getId(), userId, order.getRound());
        if (task == null) {
            throw new CustomException("您没有待处理的审批任务");
        }
        User target = userMapper.selectById(toUserId);
        if (target == null || !Objects.equals(target.getStatus(), WorkOrderConst.USER_ENABLED)) {
            throw new CustomException("转交目标用户不存在或已停用");
        }

        String comment = "转交给 " + target.getName();
        int rows = taskMapper.transferTask(task.getId(), toUserId, comment);
        if (rows == 0) {
            throw new CustomException("该审批任务已被处理，请刷新页面后重试");
        }
        ApprovalFlowNode node = flowService.requireNode(task.getNodeId());
        record(order, task, node, WorkOrderConst.ACTION_TRANSFER, comment);

        if (userId.equals(order.getApproverId())) {
            order.setApproverId(toUserId);
            workOrderMapper.updateById(order);
        }
    }

    public void addSign(WorkOrder order, List<Integer> userIds) {
        Integer userId = requireUserId();
        WorkOrderTask task = taskMapper.findUserPendingTask(order.getId(), userId, order.getRound());
        if (task == null) {
            throw new CustomException("您没有待处理的审批任务");
        }
        ApprovalFlowNode node = flowService.requireNode(task.getNodeId());

        Set<Integer> pendingApprovers = taskMapper.findPendingByNode(
                        order.getId(), order.getRound(), node.getId())
                .stream().map(WorkOrderTask::getApproverId).collect(Collectors.toSet());

        Map<Integer, User> targets = new LinkedHashMap<>();
        for (Integer id : userIds.stream().distinct().toList()) {
            User user = userMapper.selectById(id);
            if (user == null || !Objects.equals(user.getStatus(), WorkOrderConst.USER_ENABLED)) {
                throw new CustomException("加签人员不存在或已停用");
            }
            if (pendingApprovers.contains(id)) {
                continue;
            }
            targets.putIfAbsent(id, user);
        }
        if (targets.isEmpty()) {
            throw new CustomException("所选人员已在当前审批节点中");
        }

        for (User approver : targets.values()) {
            WorkOrderTask extra = new WorkOrderTask();
            extra.setWorkOrderId(order.getId());
            extra.setFlowId(order.getFlowId());
            extra.setNodeId(node.getId());
            extra.setNodeOrder(node.getNodeOrder());
            extra.setApproverId(approver.getId());
            extra.setStatus(WorkOrderConst.TASK_PENDING);
            extra.setRound(order.getRound());
            extra.setAddSign(1);
            taskMapper.insert(extra);
        }

        String names = targets.values().stream().map(User::getName).collect(Collectors.joining("、"));
        record(order, task, node, WorkOrderConst.ACTION_ADD_SIGN, "加签：" + names);

        order.setStatus(WorkOrderConst.STATUS_PROCESSING);
        order.setApproverId(firstPendingApprover(order, node));
        workOrderMapper.updateById(order);
    }

    /* ==================== 私有方法 ==================== */

    private void record(WorkOrder order, WorkOrderTask task, ApprovalFlowNode node, int action, String comment) {
        WorkOrderApproval approval = new WorkOrderApproval();
        approval.setWorkOrderId(order.getId());
        approval.setTaskId(task.getId());
        approval.setNodeId(node.getId());
        approval.setNodeName(node.getNodeName());
        approval.setRound(task.getRound());
        approval.setApproverId(requireUserId());
        approval.setAction(action);
        approval.setComment(comment);
        approvalMapper.insert(approval);
    }

    private Integer firstPendingApprover(WorkOrder order, ApprovalFlowNode node) {
        List<WorkOrderTask> pending = taskMapper.findPendingByNode(
                order.getId(), order.getRound(), node.getId());
        return pending.isEmpty() ? order.getApproverId() : pending.get(0).getApproverId();
    }

    private Integer requireUserId() {
        Integer userId = userContext.getUserId();
        if (userId == null) {
            throw new CustomException("登录已失效，请重新登录");
        }
        return userId;
    }
}
