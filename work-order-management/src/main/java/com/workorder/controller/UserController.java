package com.workorder.controller;

import com.workorder.common.Result;
import com.workorder.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户")
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "当前登录用户信息")
    @GetMapping("/profile")
    public Result profile() {
        return Result.success(userService.profile());
    }

    @Operation(summary = "用户列表", description = "用于工单关联人展示，支持姓名/用户名关键字检索")
    @GetMapping
    public Result findAll(@RequestParam(required = false) String keyword) {
        return Result.success(userService.findAll(keyword));
    }
}
