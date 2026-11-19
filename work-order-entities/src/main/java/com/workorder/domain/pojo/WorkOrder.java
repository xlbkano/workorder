package com.workorder.domain.pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("work_order")
public class WorkOrder {

    @TableId(type = IdType.AUTO)
    private Integer id;
    /** 工单号 */
    private String orderNo;
    private String title;
    /** 类型：1请假 2报销 3采购 4用印 5其他 */
    private Integer type;
    /** 优先级：1低 2中 3高 */
    private Integer priority;
    private String content;
    /** 申请人 */
    private Integer applicantId;
    /** 申请人部门 */
    private Integer applicantDeptId;
    /** 当前审批人 */
    private Integer approverId;
    /** 状态：1待审批 2审批中 3已通过 4已驳回 5已撤销 */
    private Integer status;
    /** 使用的审批流程 */
    private Integer flowId;
    /** 当前审批节点顺序 */
    private Integer currentNodeOrder;
    /** 审批轮次，重新提交时 +1 */
    private Integer round;
    /** 客户端幂等标识（唯一索引） */
    @JsonIgnore
    private String requestId;
    /** 乐观锁版本号 */
    @Version
    private Integer version;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String applicantName;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String approverName;
    @TableField(exist = false)
    private String flowName;
    @TableField(exist = false)
    private String currentNodeName;
}
