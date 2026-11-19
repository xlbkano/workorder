package com.workorder.service.impl;

import cn.hutool.core.util.StrUtil;
import com.workorder.common.WorkOrderConst;
import com.workorder.domain.dto.FlowNodeDto;
import com.workorder.domain.dto.FlowSaveDto;
import com.workorder.domain.pojo.ApprovalFlow;
import com.workorder.domain.pojo.ApprovalFlowNode;
import com.workorder.domain.pojo.Dept;
import com.workorder.domain.pojo.User;
import com.workorder.domain.pojo.WorkOrder;
import com.workorder.exception.CustomException;
import com.workorder.mapper.ApprovalFlowMapper;
import com.workorder.mapper.ApprovalFlowNodeMapper;
import com.workorder.mapper.DeptMapper;
import com.workorder.mapper.UserMapper;
import com.workorder.mapper.WorkOrderMapper;
import com.workorder.service.ApprovalFlowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApprovalFlowServiceImpl implements ApprovalFlowService {

    private static final Set<String> ROLES = Set.of(
            WorkOrderConst.ROLE_EMPLOYEE, WorkOrderConst.ROLE_MANAGER, WorkOrderConst.ROLE_ADMIN);

    @Autowired
    private ApprovalFlowMapper flowMapper;

    @Autowired
    private ApprovalFlowNodeMapper nodeMapper;

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DeptMapper deptMapper;

    @Override
    public List<ApprovalFlow> findAll(String keyword) {
        List<ApprovalFlow> flows = flowMapper.findAll(keyword);
        Map<Integer, String> deptNames = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(Dept::getId, Dept::getName));
        for (ApprovalFlow flow : flows) {
            flow.setNodes(nodesOf(flow.getId()));
            flow.setBizTypeName(flow.getBizType() == null
                    ? "通用" : WorkOrderConst.typeName(flow.getBizType()));
            flow.setDeptName(flow.getDeptId() == null
                    ? "不限部门" : deptNames.getOrDefault(flow.getDeptId(), "未知部门"));
        }
        return flows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalFlow saveFlow(FlowSaveDto dto) {
        validateFlowBase(dto);
        validateNodes(dto.getNodes());

        ApprovalFlow flow;
        if (dto.getId() == null) {
            flow = new ApprovalFlow();
        } else {
            flow = flowMapper.selectById(dto.getId());
            if (flow == null) {
                throw new CustomException("流程不存在");
            }
            long activeOrders = workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                    .eq(WorkOrder::getFlowId, dto.getId())
                    .in(WorkOrder::getStatus,
                            WorkOrderConst.STATUS_PENDING, WorkOrderConst.STATUS_PROCESSING));
            if (activeOrders > 0) {
                throw new CustomException("该流程存在审批中的工单，暂不能修改");
            }
        }

        flow.setName(dto.getName().trim());
        flow.setCode(dto.getCode().trim());
        flow.setBizType(dto.getBizType());
        flow.setDeptId(dto.getDeptId());
        flow.setStatus(dto.getStatus() == null ? WorkOrderConst.FLOW_ENABLED : dto.getStatus());
        flow.setDescription(dto.getDescription());

        if (flow.getId() == null) {
            flowMapper.insert(flow);
        } else {
            flowMapper.updateById(flow);
        }

        saveNodes(flow.getId(), dto.getNodes());
        return requireFlowById(flow.getId());
    }

    @Override
    public void updateStatus(Integer id, Integer status) {
        if (status == null || (status != 0 && status != WorkOrderConst.FLOW_ENABLED)) {
            throw new CustomException("流程状态不合法");
        }
        ApprovalFlow flow = flowMapper.selectById(id);
        if (flow == null) {
            throw new CustomException("流程不存在");
        }
        if (status == WorkOrderConst.FLOW_ENABLED) {
            long count = flowMapper.countActiveByScope(flow.getBizType(), flow.getDeptId(), id);
            if (count > 0) {
                throw new CustomException("相同适用范围的流程已启用，请先停用原流程");
            }
        }
        flow.setStatus(status);
        flowMapper.updateById(flow);
    }

    @Override
    public ApprovalFlow requireFlow(Integer bizType, Integer deptId) {
        ApprovalFlow flow = flowMapper.matchFlow(bizType, deptId);
        if (flow == null) {
            throw new CustomException("未配置审批流程，请联系管理员");
        }
        flow.setNodes(nodesOf(flow.getId()));
        if (flow.getNodes().isEmpty()) {
            throw new CustomException("审批流程未配置审批节点，请联系管理员");
        }
        return flow;
    }

    @Override
    public ApprovalFlow requireFlowById(Integer id) {
        ApprovalFlow flow = flowMapper.selectById(id);
        if (flow == null) {
            throw new CustomException("审批流程不存在");
        }
        flow.setNodes(nodesOf(id));
        return flow;
    }

    @Override
    public List<ApprovalFlowNode> nodesOf(Integer flowId) {
        return nodeMapper.selectList(new LambdaQueryWrapper<ApprovalFlowNode>()
                .eq(ApprovalFlowNode::getFlowId, flowId)
                .orderByAsc(ApprovalFlowNode::getNodeOrder));
    }

    @Override
    public ApprovalFlowNode requireNode(Integer nodeId) {
        ApprovalFlowNode node = nodeMapper.selectById(nodeId);
        if (node == null) {
            throw new CustomException("审批节点不存在");
        }
        return node;
    }

    @Override
    public List<User> resolveApprovers(ApprovalFlowNode node, User applicant) {
        if (applicant == null || applicant.getDeptId() == null) {
            throw new CustomException("申请人部门信息异常");
        }
        List<User> candidates = new ArrayList<>();
        switch (node.getApproverType()) {
            case WorkOrderConst.APPROVER_DEPT_MANAGER -> addIfNotNull(candidates,
                    userMapper.findByDeptIdAndRole(applicant.getDeptId(), WorkOrderConst.ROLE_MANAGER));
            case WorkOrderConst.APPROVER_DEPT_ROLE -> candidates.addAll(userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getDeptId, applicant.getDeptId())
                            .eq(User::getRole, node.getApproverValue())
                            .eq(User::getStatus, WorkOrderConst.USER_ENABLED)
                            .orderByAsc(User::getId)));
            case WorkOrderConst.APPROVER_ROLE -> candidates.addAll(userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getRole, node.getApproverValue())
                            .eq(User::getStatus, WorkOrderConst.USER_ENABLED)
                            .orderByAsc(User::getId)));
            case WorkOrderConst.APPROVER_USER -> addIfNotNull(candidates,
                    userMapper.selectById(parseUserId(node.getApproverValue())));
            case WorkOrderConst.APPROVER_USER_LIST -> candidates.addAll(
                    userMapper.selectBatchIds(parseUserIds(node.getApproverValue())));
            default -> throw new CustomException("不支持的审批人类型：" + node.getApproverType());
        }

        boolean allowSelf = Objects.equals(node.getAllowSelf(), 1);
        Map<Integer, User> result = new LinkedHashMap<>();
        for (User user : candidates) {
            if (user == null || !Objects.equals(user.getStatus(), WorkOrderConst.USER_ENABLED)) {
                continue;
            }
            if (!allowSelf && user.getId().equals(applicant.getId())) {
                continue;
            }
            result.putIfAbsent(user.getId(), user);
        }

        if (result.isEmpty() && !Objects.equals(node.getOptionalNode(), 1)) {
            throw new CustomException("节点【" + node.getNodeName() + "】未配置可用审批人");
        }
        return new ArrayList<>(result.values());
    }

    /* ==================== 私有方法 ==================== */

    private void validateFlowBase(FlowSaveDto dto) {
        long sameCode = flowMapper.selectCount(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getCode, dto.getCode().trim())
                .ne(dto.getId() != null, ApprovalFlow::getId, dto.getId()));
        if (sameCode > 0) {
            throw new CustomException("流程编码已存在");
        }

        if (dto.getDeptId() != null && deptMapper.selectById(dto.getDeptId()) == null) {
            throw new CustomException("适用部门不存在");
        }

        int status = dto.getStatus() == null ? WorkOrderConst.FLOW_ENABLED : dto.getStatus();
        if (status == WorkOrderConst.FLOW_ENABLED) {
            long active = flowMapper.countActiveByScope(dto.getBizType(), dto.getDeptId(), dto.getId());
            if (active > 0) {
                throw new CustomException("相同适用范围的流程已存在启用版本");
            }
        }
    }

    private void validateNodes(List<FlowNodeDto> nodes) {
        Set<Integer> orders = new HashSet<>();
        for (FlowNodeDto node : nodes) {
            if (!orders.add(node.getNodeOrder())) {
                throw new CustomException("节点顺序不能重复");
            }
            switch (node.getApproverType()) {
                case WorkOrderConst.APPROVER_DEPT_MANAGER -> node.setApproverValue(null);
                case WorkOrderConst.APPROVER_DEPT_ROLE, WorkOrderConst.APPROVER_ROLE -> {
                    if (!ROLES.contains(node.getApproverValue())) {
                        throw new CustomException("节点【" + node.getNodeName() + "】的角色配置不合法");
                    }
                }
                case WorkOrderConst.APPROVER_USER -> {
                    User user = requireEnabledUser(parseUserId(node.getApproverValue()));
                    node.setApproverValue(String.valueOf(user.getId()));
                }
                case WorkOrderConst.APPROVER_USER_LIST -> {
                    List<Integer> ids = parseUserIds(node.getApproverValue());
                    for (Integer id : ids) {
                        requireEnabledUser(id);
                    }
                    node.setApproverValue(ids.stream()
                            .map(String::valueOf).collect(Collectors.joining(",")));
                }
                default -> throw new CustomException("节点【" + node.getNodeName() + "】的审批人类型不合法");
            }
        }
    }

    private void saveNodes(Integer flowId, List<FlowNodeDto> dtoNodes) {
        List<ApprovalFlowNode> existing = nodesOf(flowId);
        Map<Integer, ApprovalFlowNode> existingMap = existing.stream()
                .collect(Collectors.toMap(ApprovalFlowNode::getId, node -> node));
        Set<Integer> keptIds = new HashSet<>();

        for (FlowNodeDto dto : dtoNodes) {
            ApprovalFlowNode node;
            if (dto.getId() != null && existingMap.containsKey(dto.getId())) {
                node = existingMap.get(dto.getId());
                keptIds.add(node.getId());
            } else {
                node = new ApprovalFlowNode();
            }
            node.setFlowId(flowId);
            node.setNodeOrder(dto.getNodeOrder());
            node.setNodeName(dto.getNodeName().trim());
            node.setApproverType(dto.getApproverType());
            node.setApproverValue(dto.getApproverValue());
            node.setMultiMode(dto.getMultiMode());
            node.setAllowSelf(dto.getAllowSelf() == null ? 0 : dto.getAllowSelf());
            node.setOptionalNode(dto.getOptionalNode() == null ? 0 : dto.getOptionalNode());
            if (node.getId() == null) {
                nodeMapper.insert(node);
            } else {
                nodeMapper.updateById(node);
            }
        }

        List<Integer> removed = existing.stream()
                .map(ApprovalFlowNode::getId)
                .filter(id -> !keptIds.contains(id))
                .toList();
        if (!removed.isEmpty()) {
            nodeMapper.deleteBatchIds(removed);
        }
    }

    private User requireEnabledUser(Integer userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getStatus(), WorkOrderConst.USER_ENABLED)) {
            throw new CustomException("审批人不存在或已停用");
        }
        return user;
    }

    private Integer parseUserId(String value) {
        if (StrUtil.isBlank(value)) {
            throw new CustomException("指定用户的审批人不能为空");
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new CustomException("审批人配置格式不正确");
        }
    }

    private List<Integer> parseUserIds(String value) {
        if (StrUtil.isBlank(value)) {
            throw new CustomException("加签人员不能为空");
        }
        List<Integer> ids = new ArrayList<>();
        for (String part : value.split(",")) {
            if (StrUtil.isBlank(part)) {
                continue;
            }
            try {
                ids.add(Integer.valueOf(part.trim()));
            } catch (NumberFormatException e) {
                throw new CustomException("审批人配置格式不正确");
            }
        }
        if (ids.isEmpty()) {
            throw new CustomException("审批人配置不能为空");
        }
        return ids.stream().distinct().toList();
    }

    private void addIfNotNull(List<User> list, User user) {
        if (user != null) {
            list.add(user);
        }
    }
}
