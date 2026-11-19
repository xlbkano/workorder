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
@TableName("approval_flow_node")
public class ApprovalFlowNode {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer flowId;
    /** 节点顺序，从 1 开始 */
    private Integer nodeOrder;
    private String nodeName;
    /** DEPT_MANAGER / DEPT_ROLE / ROLE / USER / USER_LIST */
    private String approverType;
    /** 角色标识或用户 ID（多人逗号分隔） */
    private String approverValue;
    /** OR-或签，AND-会签 */
    private String multiMode;
    /** 是否允许申请人自己审批：1 允许，0 不允许 */
    private Integer allowSelf;
    /** 找不到审批人时是否自动跳过：1 跳过，0 报错 */
    private Integer optionalNode;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
