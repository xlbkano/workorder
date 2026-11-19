package com.workorder.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("operate_log")
public class OperateLog {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer operateUserId;
    /** 操作说明 */
    private String description;
    private String className;
    private String methodName;
    private String methodParams;
    private String returnValue;
    private Long costTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operateTime;

    @TableField(exist = false)
    private String operateUserName;
}
