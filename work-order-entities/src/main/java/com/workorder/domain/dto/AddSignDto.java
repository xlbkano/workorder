package com.workorder.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AddSignDto {

    @NotEmpty(message = "请选择加签人员")
    @Size(max = 10, message = "一次最多加签 10 人")
    private List<@NotNull(message = "加签人员不合法") @Min(value = 1, message = "加签人员不合法") Integer> userIds;
}
