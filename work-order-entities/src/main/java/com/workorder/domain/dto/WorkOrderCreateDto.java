package com.workorder.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkOrderCreateDto {

    @NotBlank(message = "工单标题不能为空")
    @Size(min = 2, max = 50, message = "工单标题长度须为 2-50 个字符")
    private String title;

    @NotNull(message = "工单类型不能为空")
    @Min(value = 1, message = "工单类型不合法")
    @Max(value = 5, message = "工单类型不合法")
    private Integer type;

    @NotNull(message = "优先级不能为空")
    @Min(value = 1, message = "优先级不合法")
    @Max(value = 3, message = "优先级不合法")
    private Integer priority;

    @NotBlank(message = "工单内容不能为空")
    @Size(min = 5, max = 1000, message = "工单内容长度须为 5-1000 个字符")
    private String content;
}
