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
@TableName("work_order_task")
public class WorkOrderTask {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer workOrderId;
    private Integer flowId;
    private Integer nodeId;
    private Integer nodeOrder;
    private Integer approverId;
    /** 0-待处理 1-已通过 2-已驳回 3-已转交 4-已失效 */
    private Integer status;
    /** 审批轮次 */
    private Integer round;
    /** 是否加签任务：1 是，0 否 */
    private Integer addSign;
    private String comment;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String approverName;
    @TableField(exist = false)
    private String nodeName;
}
