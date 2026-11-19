package com.workorder.service.impl;

import com.workorder.common.PageData;
import com.workorder.domain.pojo.OperateLog;
import com.workorder.mapper.OperateLogMapper;
import com.workorder.service.LogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LogServiceImpl implements LogService {

    @Autowired
    private OperateLogMapper operateLogMapper;

    @Override
    public void save(OperateLog operateLog) {
        operateLogMapper.insert(operateLog);
    }

    @Override
    public PageData<OperateLog> findPage(Integer page, Integer size, String keyword) {
        Page<OperateLog> result = operateLogMapper.findPage(new Page<>(page, size), keyword);
        return new PageData<>(result.getTotal(), result.getRecords(), null);
    }
}
