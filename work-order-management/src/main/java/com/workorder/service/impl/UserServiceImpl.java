package com.workorder.service.impl;

import com.workorder.common.JwtUtil;
import com.workorder.common.UserContext;
import com.workorder.common.WorkOrderConst;
import com.workorder.domain.dto.LoginDto;
import com.workorder.domain.pojo.User;
import com.workorder.domain.vo.LoginVo;
import com.workorder.domain.vo.UserVo;
import com.workorder.exception.CustomException;
import com.workorder.mapper.UserMapper;
import com.workorder.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserContext userContext;

    @Override
    public LoginVo login(LoginDto dto) {
        User user = userMapper.findByUsername(dto.getUsername());
        if (user == null || !dto.getPassword().equals(user.getPassword())) {
            throw new CustomException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != WorkOrderConst.USER_ENABLED) {
            throw new CustomException("账号已停用，请联系管理员");
        }
        if (user.getDeptId() == null) {
            throw new CustomException("账号未分配部门，请联系管理员");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        claims.put("name", user.getName());
        claims.put("role", user.getRole());
        claims.put("deptId", user.getDeptId());
        String token = jwtUtil.makeToken(claims);
        return new LoginVo(token, toVo(user));
    }

    @Override
    public UserVo profile() {
        Integer userId = userContext.getUserId();
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            throw new CustomException("用户不存在或已被删除");
        }
        return toVo(user);
    }

    @Override
    public List<UserVo> findAll(String keyword) {
        return userMapper.findAll(keyword).stream().map(this::toVo).toList();
    }

    @Override
    public UserVo toVo(User user) {
        return new UserVo(user.getId(), user.getUsername(), user.getName(),
                user.getRole(), user.getDeptId(), user.getDeptName());
    }
}
