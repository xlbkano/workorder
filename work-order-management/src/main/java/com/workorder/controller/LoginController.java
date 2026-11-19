package com.workorder.controller;

import com.workorder.common.Result;
import com.workorder.domain.dto.LoginDto;
import com.workorder.domain.vo.LoginVo;
import com.workorder.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "登录认证")
@RestController
public class LoginController {

    @Autowired
    private UserService userService;

    @Operation(summary = "登录", description = "校验用户名密码，返回 JWT token 与用户信息")
    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginDto dto) {
        LoginVo loginVo = userService.login(dto);
        return Result.success(loginVo);
    }
}
