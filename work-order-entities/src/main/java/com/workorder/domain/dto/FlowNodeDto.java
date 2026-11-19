package com.workorder.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FlowNodeDto {

    private Integer id;

    @NotNull(message = "节点顺序不能为空")
    @Min(value = 1, message = "节点顺序不能小于 1")
    private Integer nodeOrder;

    @NotBlank(message = "节点名称不能为空")
    @Size(max = 30, message = "节点名称不能超过 30 个字符")
    private String nodeName;

    @NotBlank(message = "审批人类型不能为空")
    @Pattern(regexp = "DEPT_MANAGER|DEPT_ROLE|ROLE|USER|USER_LIST", message = "审批人类型不合法")
    private String approverType;

    @Size(max = 200, message = "审批人配置不能超过 200 个字符")
    private String approverValue;

    @NotBlank(message = "签核方式不能为空")
    @Pattern(regexp = "OR|AND", message = "签核方式不合法")
    private String multiMode;

    @Min(value = 0, message = "allowSelf 不合法")
    private Integer allowSelf = 0;

    @Min(value = 0, message = "optionalNode 不合法")
    private Integer optionalNode = 0;
}
