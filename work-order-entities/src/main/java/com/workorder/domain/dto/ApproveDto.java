package com.workorder.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApproveDto {

    /** 1-通过 2-驳回（驳回时审批意见必填，由业务校验） */
    @NotNull(message = "审批动作不能为空")
    @Min(value = 1, message = "审批动作不合法")
    @Max(value = 2, message = "审批动作不合法")
    private Integer action;

    @Size(max = 200, message = "审批意见不能超过 200 个字符")
    private String comment;
}
