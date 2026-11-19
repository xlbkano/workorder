package com.workorder.mapper;

import com.workorder.domain.pojo.WorkOrderApproval;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkOrderApprovalMapper extends BaseMapper<WorkOrderApproval> {

    List<WorkOrderApproval> findByOrderId(@Param("orderId") Integer orderId);
}
