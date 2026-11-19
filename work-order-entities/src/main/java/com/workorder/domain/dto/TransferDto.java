package com.workorder.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferDto {

    @NotNull(message = "转交目标用户不能为空")
    @Min(value = 1, message = "转交目标用户不合法")
    private Integer toUserId;
}
