package com.workorder.mapper;

import com.workorder.domain.pojo.ApprovalFlow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApprovalFlowMapper extends BaseMapper<ApprovalFlow> {

    /**
     * 按工单类型 + 部门匹配启用中的流程：类型精确 > 类型通用，部门精确 > 部门不限
     */
    ApprovalFlow matchFlow(@Param("bizType") Integer bizType, @Param("deptId") Integer deptId);

    List<ApprovalFlow> findAll(@Param("keyword") String keyword);

    long countActiveByScope(@Param("bizType") Integer bizType,
                            @Param("deptId") Integer deptId,
                            @Param("excludeId") Integer excludeId);
}
