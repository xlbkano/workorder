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
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("approval_flow")
public class ApprovalFlow {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    /** 唯一编码 */
    private String code;
    /** 适用工单类型，null 表示通用流程 */
    private Integer bizType;
    /** 适用部门，null 表示不限部门 */
    private Integer deptId;
    /** 1-启用，0-停用 */
    private Integer status;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<ApprovalFlowNode> nodes;
    @TableField(exist = false)
    private String bizTypeName;
    @TableField(exist = false)
    private String deptName;
}
