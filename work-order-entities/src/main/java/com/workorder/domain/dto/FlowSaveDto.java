package com.workorder.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class FlowSaveDto {

    private Integer id;

    @NotBlank(message = "流程名称不能为空")
    @Size(max = 30, message = "流程名称不能超过 30 个字符")
    private String name;

    @NotBlank(message = "流程编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]{2,30}$", message = "流程编码须为 2-30 位字母、数字或下划线")
    private String code;

    /** 适用工单类型，null 表示通用流程 */
    @Min(value = 1, message = "适用类型不合法")
    @Max(value = 5, message = "适用类型不合法")
    private Integer bizType;

    /** 适用部门，null 表示不限部门 */
    @Min(value = 1, message = "适用部门不合法")
    private Integer deptId;

    @Min(value = 0, message = "流程状态不合法")
    @Max(value = 1, message = "流程状态不合法")
    private Integer status = 1;

    @Size(max = 100, message = "流程说明不能超过 100 个字符")
    private String description;

    @Valid
    @NotEmpty(message = "流程至少需要一个审批节点")
    @Size(max = 10, message = "流程节点不能超过 10 个")
    private List<FlowNodeDto> nodes;
}
