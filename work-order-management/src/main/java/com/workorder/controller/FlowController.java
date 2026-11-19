package com.workorder.controller;

import com.workorder.common.Result;
import com.workorder.common.UserContext;
import com.workorder.common.WorkOrderConst;
import com.workorder.domain.dto.FlowSaveDto;
import com.workorder.domain.pojo.ApprovalFlow;
import com.workorder.exception.CustomException;
import com.workorder.service.ApprovalFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "流程配置")
@RestController
@RequestMapping("/flows")
public class FlowController {

    @Autowired
    private ApprovalFlowService flowService;

    @Autowired
    private UserContext userContext;

    @Operation(summary = "流程列表", description = "仅管理员可访问，返回流程及其节点配置")
    @GetMapping
    public Result findAll(@RequestParam(required = false) String keyword) {
        requireAdmin();
        List<ApprovalFlow> flows = flowService.findAll(keyword);
        return Result.success(flows);
    }

    @Operation(summary = "保存流程", description = "新建或编辑流程及节点；存在审批中工单时不允许修改")
    @PostMapping
    public Result save(@Valid @RequestBody FlowSaveDto dto) {
        requireAdmin();
        ApprovalFlow flow = flowService.saveFlow(dto);
        return Result.success("保存成功", flow);
    }

    @Operation(summary = "启用/停用流程")
    @PutMapping("/{id}/status")
    public Result updateStatus(@PathVariable Integer id, @RequestParam Integer status) {
        requireAdmin();
        flowService.updateStatus(id, status);
        return Result.success();
    }

    private void requireAdmin() {
        if (!WorkOrderConst.ROLE_ADMIN.equals(userContext.getRole())) {
            throw new CustomException("无权限操作流程配置");
        }
    }
}
