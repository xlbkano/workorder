package com.workorder.mapper;

import com.workorder.domain.pojo.OperateLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OperateLogMapper extends BaseMapper<OperateLog> {

    Page<OperateLog> findPage(Page<OperateLog> page, @Param("keyword") String keyword);
}
