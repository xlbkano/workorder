package com.workorder.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkOrderQueryDto {

    @Min(value = 1, message = "页码不能小于 1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Integer size = 10;

    @Min(value = 1, message = "状态不合法")
    @Max(value = 5, message = "状态不合法")
    private Integer status;

    @Min(value = 1, message = "工单类型不合法")
    @Max(value = 5, message = "工单类型不合法")
    private Integer type;

    @Size(max = 20, message = "关键字不能超过 20 个字符")
    private String keyword;

    /** 游标分页：上一页最后一条记录的 id，传入则使用游标模式 */
    @Min(value = 1, message = "游标参数不合法")
    private Long lastId;
}
