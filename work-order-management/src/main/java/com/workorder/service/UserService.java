package com.workorder.service;

import com.workorder.domain.dto.LoginDto;
import com.workorder.domain.pojo.User;
import com.workorder.domain.vo.LoginVo;
import com.workorder.domain.vo.UserVo;

import java.util.List;

public interface UserService {

    LoginVo login(LoginDto dto);

    UserVo profile();

    List<UserVo> findAll(String keyword);

    UserVo toVo(User user);
}
