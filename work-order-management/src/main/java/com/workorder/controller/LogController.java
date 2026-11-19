package com.workorder.controller;

import com.workorder.common.PageData;
import com.workorder.common.Result;
import com.workorder.common.UserContext;
import com.workorder.common.WorkOrderConst;
import com.workorder.domain.pojo.OperateLog;
import com.workorder.exception.CustomException;
import com.workorder.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "操作日志")
@Validated
@RestController
@RequestMapping("/logs")
public class LogController {

    @Autowired
    private LogService logService;

    @Autowired
    private UserContext userContext;

    @Operation(summary = "操作日志分页查询", description = "仅管理员可查看，记录工单提交、审批、撤销等关键操作")
    @GetMapping
    public Result findByPage(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于 1") Integer page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不合法")
            @Max(value = 100, message = "每页条数不能超过 100") Integer size,
            @RequestParam(required = false) String keyword) {
        if (!WorkOrderConst.ROLE_ADMIN.equals(userContext.getRole())) {
            throw new CustomException("无权限查看操作日志");
        }
        PageData<OperateLog> data = logService.findPage(page, size, keyword);
        return Result.success(data);
    }
}
