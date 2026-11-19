package com.workorder.controller;

import com.workorder.aspect.Idempotent;
import com.workorder.aspect.LogRecord;
import com.workorder.common.PageData;
import com.workorder.common.Result;
import com.workorder.domain.dto.AddSignDto;
import com.workorder.domain.dto.ApproveDto;
import com.workorder.domain.dto.TransferDto;
import com.workorder.domain.dto.WorkOrderCreateDto;
import com.workorder.domain.dto.WorkOrderQueryDto;
import com.workorder.domain.pojo.WorkOrder;
import com.workorder.domain.vo.StatsVo;
import com.workorder.domain.vo.WorkOrderDetailVo;
import com.workorder.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "工单管理")
@Validated
@RestController
@RequestMapping("/work-orders")
public class WorkOrderController {

    @Autowired
    private WorkOrderService workOrderService;

    @Operation(summary = "工单列表分页查询",
            description = "支持状态、类型、关键字筛选；传 lastId 时使用游标分页，解决深分页性能问题")
    @GetMapping
    public Result findByPage(@Valid WorkOrderQueryDto query) {
        PageData<WorkOrder> page = workOrderService.findByPage(query);
        return Result.success(page);
    }

    @Operation(summary = "待我审批的工单")
    @GetMapping("/todo")
    public Result findTodo(@Valid WorkOrderQueryDto query) {
        PageData<WorkOrder> page = workOrderService.findTodo(query);
        return Result.success(page);
    }

    @Operation(summary = "工单统计", description = "按状态、类型、部门统计当前用户数据权限范围内的工单")
    @GetMapping("/stats")
    public Result stats() {
        StatsVo statsVo = workOrderService.stats();
        return Result.success(statsVo);
    }

    @Operation(summary = "工单详情", description = "返回工单信息、审批记录以及当前登录人的操作权限")
    @GetMapping("/{id}")
    public Result detail(@PathVariable Integer id) {
        WorkOrderDetailVo detail = workOrderService.detail(id);
        return Result.success(detail);
    }

    @Operation(summary = "提交工单",
            description = "请求头 X-Request-Id 为幂等标识；参数经 Bean Validation 与业务校验后入库")
    @PostMapping
    @Idempotent
    @LogRecord("提交工单")
    public Result add(@Parameter(description = "客户端生成的幂等标识")
                      @RequestHeader(value = "X-Request-Id", required = false) String requestId,
                      @Valid @RequestBody WorkOrderCreateDto dto) {
        WorkOrder order = workOrderService.create(dto, requestId);
        return Result.success("工单提交成功", order);
    }

    @Operation(summary = "审批工单", description = "action：1-通过，2-驳回；驳回时审批意见必填")
    @PostMapping("/{id}/approve")
    @LogRecord("审批工单")
    public Result approve(@PathVariable Integer id, @Valid @RequestBody ApproveDto dto) {
        workOrderService.approve(id, dto);
        return Result.success("审批完成", null);
    }

    @Operation(summary = "撤销工单", description = "仅申请人可撤销待审批/审批中的工单")
    @PutMapping("/{id}/withdraw")
    @LogRecord("撤销工单")
    public Result withdraw(@PathVariable Integer id) {
        workOrderService.withdraw(id);
        return Result.success("工单已撤销", null);
    }

    @Operation(summary = "转交审批", description = "当前审批人可将审批任务转交给其他用户")
    @PostMapping("/{id}/transfer")
    @LogRecord("转交工单")
    public Result transfer(@PathVariable Integer id, @Valid @RequestBody TransferDto dto) {
        workOrderService.transfer(id, dto);
        return Result.success("已转交", null);
    }

    @Operation(summary = "加签", description = "当前审批人在当前节点追加审批人，追加人通过后才可推进")
    @PostMapping("/{id}/add-sign")
    @LogRecord("加签")
    public Result addSign(@PathVariable Integer id, @Valid @RequestBody AddSignDto dto) {
        workOrderService.addSign(id, dto);
        return Result.success("加签成功", null);
    }

    @Operation(summary = "重新提交", description = "被驳回的工单可由申请人重新提交，轮次 +1，历史记录保留")
    @PostMapping("/{id}/resubmit")
    @LogRecord("重新提交工单")
    public Result resubmit(@PathVariable Integer id) {
        workOrderService.resubmit(id);
        return Result.success("已重新提交", null);
    }
}
