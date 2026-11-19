package com.workorder.service;

import com.workorder.domain.dto.FlowSaveDto;
import com.workorder.domain.pojo.ApprovalFlow;
import com.workorder.domain.pojo.ApprovalFlowNode;
import com.workorder.domain.pojo.User;

import java.util.List;

public interface ApprovalFlowService {

    List<ApprovalFlow> findAll(String keyword);

    ApprovalFlow saveFlow(FlowSaveDto dto);

    void updateStatus(Integer id, Integer status);

    ApprovalFlow requireFlow(Integer bizType, Integer deptId);

    ApprovalFlow requireFlowById(Integer id);

    List<ApprovalFlowNode> nodesOf(Integer flowId);

    ApprovalFlowNode requireNode(Integer nodeId);

    /**
     * 解析节点审批人；节点为可选且无人可用时返回空集合，否则抛出业务异常
     */
    List<User> resolveApprovers(ApprovalFlowNode node, User applicant);
}
