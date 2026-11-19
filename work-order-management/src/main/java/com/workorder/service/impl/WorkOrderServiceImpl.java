package com.workorder.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.workorder.common.PageData;
import com.workorder.common.UserContext;
import com.workorder.common.WorkOrderConst;
import com.workorder.common.WorkOrderStatus;
import com.workorder.domain.dto.AddSignDto;
import com.workorder.domain.dto.ApproveDto;
import com.workorder.domain.dto.TransferDto;
import com.workorder.domain.dto.WorkOrderCreateDto;
import com.workorder.domain.dto.WorkOrderQueryDto;
import com.workorder.domain.pojo.ApprovalFlow;
import com.workorder.domain.pojo.User;
import com.workorder.domain.pojo.WorkOrder;
import com.workorder.domain.pojo.WorkOrderApproval;
import com.workorder.domain.pojo.WorkOrderTask;
import com.workorder.domain.vo.StatsVo;
import com.workorder.domain.vo.WorkOrderDetailVo;
import com.workorder.exception.CustomException;
import com.workorder.mapper.UserMapper;
import com.workorder.mapper.WorkOrderApprovalMapper;
import com.workorder.mapper.WorkOrderMapper;
import com.workorder.mapper.WorkOrderTaskMapper;
import com.workorder.service.ApprovalEngine;
import com.workorder.service.ApprovalFlowService;
import com.workorder.service.WorkOrderService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    private static final String SCOPE_ALL = "ALL";
    private static final String SCOPE_DEPT = "DEPT";
    private static final String SCOPE_MINE = "MINE";
    private static final String SCOPE_TODO = "TODO";

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private WorkOrderApprovalMapper approvalMapper;

    @Autowired
    private WorkOrderTaskMapper taskMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ApprovalFlowService flowService;

    @Autowired
    private ApprovalEngine approvalEngine;

    @Autowired
    private UserContext userContext;

    /* ==================== 查询 ==================== */

    @Override
    public PageData<WorkOrder> findByPage(WorkOrderQueryDto query) {
        return queryPage(query, currentScope());
    }

    @Override
    public PageData<WorkOrder> findTodo(WorkOrderQueryDto query) {
        return queryPage(query, SCOPE_TODO);
    }

    private PageData<WorkOrder> queryPage(WorkOrderQueryDto query, String scope) {
        Integer userId = currentUserId();
        Integer deptId = userContext.getDeptId();
        int size = query.getSize() == null ? 10 : query.getSize();

        // 游标分页：适用于滚动加载、深分页场景，避免 LIMIT offset 扫描
        if (query.getLastId() != null) {
            List<WorkOrder> rows = workOrderMapper.findCursor(query, size, scope, userId, deptId);
            long total = workOrderMapper.countByCondition(query, scope, userId, deptId);
            Long next = rows.isEmpty() ? null : rows.get(rows.size() - 1).getId().longValue();
            return new PageData<>(total, rows, next);
        }

        int pageNo = query.getPage() == null ? 1 : query.getPage();
        Page<WorkOrder> page = workOrderMapper.findPage(
                new Page<>(pageNo, size), query, scope, userId, deptId);
        return new PageData<>(page.getTotal(), page.getRecords(), null);
    }

    @Override
    public WorkOrderDetailVo detail(Integer id) {
        WorkOrder order = workOrderMapper.findDetail(id);
        if (order == null) {
            throw new CustomException("工单不存在");
        }
        checkReadable(order);

        List<WorkOrderApproval> approvals = approvalMapper.findByOrderId(id);
        List<WorkOrderTask> tasks = taskMapper.findByOrderId(id);

        Integer userId = currentUserId();
        WorkOrderStatus status = WorkOrderStatus.of(order.getStatus());
        boolean active = status != null && status.isActive();
        WorkOrderTask pendingTask = active
                ? taskMapper.findUserPendingTask(id, userId, order.getRound())
                : null;

        boolean isApplicant = userId.equals(order.getApplicantId());
        boolean canApprove = active && pendingTask != null;
        return new WorkOrderDetailVo(
                order,
                approvals,
                tasks,
                order.getFlowName(),
                order.getCurrentNodeName(),
                canApprove,
                canApprove,
                canApprove,
                active && isApplicant,
                isApplicant && status != null && status.canResubmit()
        );
    }

    @Override
    public StatsVo stats() {
        String scope = currentScope();
        Integer userId = currentUserId();
        Integer deptId = userContext.getDeptId();
        WorkOrderQueryDto query = new WorkOrderQueryDto();

        Map<Integer, Long> statusCounts = new HashMap<>();
        for (Map<String, Object> row : workOrderMapper.countByStatus(query, scope, userId, deptId)) {
            statusCounts.put(number(row.get("status")).intValue(),
                    number(row.get("cnt")).longValue());
        }

        StatsVo vo = new StatsVo();
        vo.setPendingCount(statusCounts.getOrDefault(WorkOrderConst.STATUS_PENDING, 0L));
        vo.setProcessingCount(statusCounts.getOrDefault(WorkOrderConst.STATUS_PROCESSING, 0L));
        vo.setApprovedCount(statusCounts.getOrDefault(WorkOrderConst.STATUS_APPROVED, 0L));
        vo.setRejectedCount(statusCounts.getOrDefault(WorkOrderConst.STATUS_REJECTED, 0L));
        vo.setWithdrawnCount(statusCounts.getOrDefault(WorkOrderConst.STATUS_WITHDRAWN, 0L));
        vo.setTotal(statusCounts.values().stream().mapToLong(Long::longValue).sum());

        List<StatsVo.StatItem> byType = new ArrayList<>();
        for (Map<String, Object> row : workOrderMapper.countByType(query, scope, userId, deptId)) {
            int type = number(row.get("type")).intValue();
            byType.add(new StatsVo.StatItem(WorkOrderConst.typeName(type),
                    number(row.get("cnt")).longValue()));
        }
        vo.setByType(byType);

        List<StatsVo.StatItem> byDept = new ArrayList<>();
        for (Map<String, Object> row : workOrderMapper.countByDept(query, scope, userId, deptId)) {
            String name = row.get("name") == null ? "未知部门" : row.get("name").toString();
            byDept.add(new StatsVo.StatItem(name, number(row.get("cnt")).longValue()));
        }
        vo.setByDept(byDept);
        return vo;
    }

    /* ==================== 提交 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder create(WorkOrderCreateDto dto, String requestId) {
        Integer userId = currentUserId();
        User applicant = userMapper.selectById(userId);
        if (applicant == null || applicant.getDeptId() == null) {
            throw new CustomException("申请人信息异常，请联系管理员");
        }

        ApprovalFlow flow = flowService.requireFlow(dto.getType(), applicant.getDeptId());
        ApprovalEngine.StartNode start = approvalEngine.resolveStart(flow, applicant);

        WorkOrder order = new WorkOrder();
        order.setOrderNo(generateOrderNo());
        order.setTitle(dto.getTitle().trim());
        order.setType(dto.getType());
        order.setPriority(dto.getPriority());
        order.setContent(dto.getContent().trim());
        order.setApplicantId(userId);
        order.setApplicantDeptId(applicant.getDeptId());
        order.setApproverId(start.approvers().get(0).getId());
        order.setStatus(WorkOrderConst.STATUS_PENDING);
        order.setFlowId(flow.getId());
        order.setCurrentNodeOrder(start.node().getNodeOrder());
        order.setRound(1);
        order.setRequestId(StrUtil.isBlank(requestId) ? IdUtil.fastSimpleUUID() : requestId);
        order.setVersion(0);
        try {
            workOrderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            throw new CustomException("请勿重复提交工单");
        }
        approvalEngine.createTasks(order, start.node(), start.approvers());
        return order;
    }

    private String generateOrderNo() {
        return "WO" + DateUtil.format(new Date(), "yyyyMMddHHmmssSSS")
                + RandomUtil.randomNumbers(3);
    }

    /* ==================== 审批动作 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Integer id, ApproveDto dto) {
        WorkOrder order = lockOrder(id);
        WorkOrderStatus status = WorkOrderStatus.of(order.getStatus());
        if (status == null || !status.canApprove()) {
            throw new CustomException("当前工单状态不允许审批");
        }
        approvalEngine.approve(order, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Integer id) {
        Integer userId = currentUserId();
        WorkOrder order = lockOrder(id);
        if (!userId.equals(order.getApplicantId())) {
            throw new CustomException("只能撤销自己提交的工单");
        }
        WorkOrderStatus status = WorkOrderStatus.of(order.getStatus());
        if (status == null || !status.canWithdraw()) {
            throw new CustomException("当前工单状态不允许撤销");
        }
        approvalEngine.withdraw(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmit(Integer id) {
        Integer userId = currentUserId();
        WorkOrder order = lockOrder(id);
        if (!userId.equals(order.getApplicantId())) {
            throw new CustomException("只能重新提交自己提交的工单");
        }
        WorkOrderStatus status = WorkOrderStatus.of(order.getStatus());
        if (status == null || !status.canResubmit()) {
            throw new CustomException("当前工单状态不允许重新提交");
        }
        User applicant = userMapper.selectById(userId);
        if (applicant == null) {
            throw new CustomException("申请人信息异常，请联系管理员");
        }
        approvalEngine.resubmit(order, applicant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Integer id, TransferDto dto) {
        WorkOrder order = lockOrder(id);
        requireActive(order, "转交");
        approvalEngine.transfer(order, dto.getToUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSign(Integer id, AddSignDto dto) {
        WorkOrder order = lockOrder(id);
        requireActive(order, "加签");
        approvalEngine.addSign(order, dto.getUserIds());
    }

    /* ==================== 权限与工具方法 ==================== */

    private WorkOrder lockOrder(Integer id) {
        WorkOrder order = workOrderMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new CustomException("工单不存在");
        }
        return order;
    }

    private void requireActive(WorkOrder order, String action) {
        WorkOrderStatus status = WorkOrderStatus.of(order.getStatus());
        if (status == null || !status.isActive()) {
            throw new CustomException("当前工单状态不允许" + action);
        }
    }

    private String currentScope() {
        String role = userContext.getRole();
        if (WorkOrderConst.ROLE_ADMIN.equals(role)) {
            return SCOPE_ALL;
        }
        if (WorkOrderConst.ROLE_MANAGER.equals(role)) {
            return SCOPE_DEPT;
        }
        return SCOPE_MINE;
    }

    private void checkReadable(WorkOrder order) {
        String role = userContext.getRole();
        if (WorkOrderConst.ROLE_ADMIN.equals(role)) {
            return;
        }
        Integer userId = currentUserId();
        if (userId.equals(order.getApplicantId()) || userId.equals(order.getApproverId())) {
            return;
        }
        if (taskMapper.countByApprover(order.getId(), userId) > 0) {
            return;
        }
        Integer deptId = userContext.getDeptId();
        if (WorkOrderConst.ROLE_MANAGER.equals(role) && deptId != null
                && deptId.equals(order.getApplicantDeptId())) {
            return;
        }
        throw new CustomException("无权查看该工单");
    }

    private Integer currentUserId() {
        Integer userId = userContext.getUserId();
        if (userId == null) {
            throw new CustomException("登录已失效，请重新登录");
        }
        return userId;
    }

    private Number number(Object value) {
        return value == null ? 0 : (Number) value;
    }
}
