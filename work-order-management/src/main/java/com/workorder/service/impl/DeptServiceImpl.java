package com.workorder.service.impl;

import com.workorder.domain.pojo.Dept;
import com.workorder.mapper.DeptMapper;
import com.workorder.service.DeptService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeptServiceImpl implements DeptService {

    @Autowired
    private DeptMapper deptMapper;

    @Override
    public List<Dept> findAll() {
        return deptMapper.selectList(new LambdaQueryWrapper<Dept>()
                .eq(Dept::getStatus, 1)
                .orderByAsc(Dept::getId));
    }
}
