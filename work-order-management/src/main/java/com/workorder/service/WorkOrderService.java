package com.workorder.service;

import com.workorder.common.PageData;
import com.workorder.domain.dto.AddSignDto;
import com.workorder.domain.dto.ApproveDto;
import com.workorder.domain.dto.TransferDto;
import com.workorder.domain.dto.WorkOrderCreateDto;
import com.workorder.domain.dto.WorkOrderQueryDto;
import com.workorder.domain.pojo.WorkOrder;
import com.workorder.domain.vo.StatsVo;
import com.workorder.domain.vo.WorkOrderDetailVo;

public interface WorkOrderService {

    PageData<WorkOrder> findByPage(WorkOrderQueryDto query);

    PageData<WorkOrder> findTodo(WorkOrderQueryDto query);

    WorkOrderDetailVo detail(Integer id);

    WorkOrder create(WorkOrderCreateDto dto, String requestId);

    void approve(Integer id, ApproveDto dto);

    void withdraw(Integer id);

    void resubmit(Integer id);

    void transfer(Integer id, TransferDto dto);

    void addSign(Integer id, AddSignDto dto);

    StatsVo stats();
}
