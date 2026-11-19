package com.workorder.mapper;

import com.workorder.domain.pojo.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    User findByUsername(@Param("username") String username);

    User findByDeptIdAndRole(@Param("deptId") Integer deptId, @Param("role") String role);

    User findByRole(@Param("role") String role);

    List<User> findAll(@Param("keyword") String keyword);
}
