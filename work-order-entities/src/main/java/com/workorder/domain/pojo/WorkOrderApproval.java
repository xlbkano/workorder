package com.workorder.domain.pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
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
@TableName("work_order_approval")
public class WorkOrderApproval {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer workOrderId;
    private Integer taskId;
    private Integer nodeId;
    private String nodeName;
    private Integer round;
    private Integer approverId;
    /** 1-通过 2-驳回 3-转交 4-加签 */
    private Integer action;
    /** 审批意见 */
    private String comment;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String approverName;
}
