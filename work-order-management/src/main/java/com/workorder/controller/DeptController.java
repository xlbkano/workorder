package com.workorder.controller;

import com.workorder.common.Result;
import com.workorder.service.DeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "部门")
@RestController
@RequestMapping("/depts")
public class DeptController {

    @Autowired
    private DeptService deptService;

    @Operation(summary = "部门列表")
    @GetMapping
    public Result findAll() {
        return Result.success(deptService.findAll());
    }
}
