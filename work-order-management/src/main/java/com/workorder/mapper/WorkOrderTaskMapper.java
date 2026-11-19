package com.workorder.mapper;

import com.workorder.domain.pojo.WorkOrderTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkOrderTaskMapper extends BaseMapper<WorkOrderTask> {

    List<WorkOrderTask> findByOrderId(@Param("orderId") Integer orderId);

    WorkOrderTask findUserPendingTask(@Param("orderId") Integer orderId,
                                      @Param("approverId") Integer approverId,
                                      @Param("round") Integer round);

    List<WorkOrderTask> findPendingByNode(@Param("orderId") Integer orderId,
                                          @Param("round") Integer round,
                                          @Param("nodeId") Integer nodeId);

    long countPendingByNode(@Param("orderId") Integer orderId,
                            @Param("round") Integer round,
                            @Param("nodeId") Integer nodeId);

    long countAddSignByNode(@Param("orderId") Integer orderId,
                            @Param("round") Integer round,
                            @Param("nodeId") Integer nodeId);

    long countByApprover(@Param("orderId") Integer orderId,
                         @Param("approverId") Integer approverId);

    int updateTaskStatus(@Param("id") Integer id,
                         @Param("status") Integer status,
                         @Param("comment") String comment);

    int invalidatePending(@Param("orderId") Integer orderId,
                          @Param("round") Integer round,
                          @Param("nodeId") Integer nodeId,
                          @Param("excludeTaskId") Integer excludeTaskId);

    int transferTask(@Param("id") Integer id,
                     @Param("toUserId") Integer toUserId,
                     @Param("comment") String comment);
}
