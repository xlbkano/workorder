package com.workorder.service;

import com.workorder.common.PageData;
import com.workorder.domain.pojo.OperateLog;

public interface LogService {

    void save(OperateLog operateLog);

    PageData<OperateLog> findPage(Integer page, Integer size, String keyword);
}
