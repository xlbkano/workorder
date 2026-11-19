package com.workorder.mapper;

import com.workorder.domain.dto.WorkOrderQueryDto;
import com.workorder.domain.pojo.WorkOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {

    /**
     * 分页列表（多表联查申请人、部门、审批人）
     * scope：ALL-全部 DEPT-本部门 MINE-我的 TODO-待我审批
     */
    Page<WorkOrder> findPage(Page<WorkOrder> page,
                             @Param("q") WorkOrderQueryDto q,
                             @Param("scope") String scope,
                             @Param("userId") Integer userId,
                             @Param("deptId") Integer deptId);

    /**
     * 游标分页：按 id 倒序，避免深分页 offset 扫描
     */
    List<WorkOrder> findCursor(@Param("q") WorkOrderQueryDto q,
                               @Param("size") Integer size,
                               @Param("scope") String scope,
                               @Param("userId") Integer userId,
                               @Param("deptId") Integer deptId);

    long countByCondition(@Param("q") WorkOrderQueryDto q,
                          @Param("scope") String scope,
                          @Param("userId") Integer userId,
                          @Param("deptId") Integer deptId);

    WorkOrder findDetail(@Param("id") Integer id);

    /**
     * 行锁读取工单，串行化审批动作，避免同一节点并发推进
     */
    WorkOrder selectByIdForUpdate(@Param("id") Integer id);

    List<Map<String, Object>> countByStatus(@Param("q") WorkOrderQueryDto q,
                                            @Param("scope") String scope,
                                            @Param("userId") Integer userId,
                                            @Param("deptId") Integer deptId);

    List<Map<String, Object>> countByType(@Param("q") WorkOrderQueryDto q,
                                          @Param("scope") String scope,
                                          @Param("userId") Integer userId,
                                          @Param("deptId") Integer deptId);

    List<Map<String, Object>> countByDept(@Param("q") WorkOrderQueryDto q,
                                          @Param("scope") String scope,
                                          @Param("userId") Integer userId,
                                          @Param("deptId") Integer deptId);
}
