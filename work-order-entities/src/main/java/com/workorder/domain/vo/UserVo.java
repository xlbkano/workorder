package com.workorder.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVo {

    private Integer id;
    private String username;
    private String name;
    private String role;
    private Integer deptId;
    private String deptName;
}
