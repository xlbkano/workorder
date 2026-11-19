package com.workorder.common;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 当前登录用户上下文（请求线程内有效）
 */
@Component
public class UserContext {

    private static final ThreadLocal<Map<String, Object>> HOLDER = new ThreadLocal<>();

    public void set(Map<String, Object> claims) {
        HOLDER.set(claims);
    }

    public Map<String, Object> get() {
        return HOLDER.get();
    }

    public boolean isLogin() {
        return HOLDER.get() != null;
    }

    public Integer getUserId() {
        return toInt(claim("id"));
    }

    public String getUserName() {
        Object name = claim("name");
        return name == null ? null : name.toString();
    }

    public String getRole() {
        Object role = claim("role");
        return role == null ? null : role.toString();
    }

    public Integer getDeptId() {
        return toInt(claim("deptId"));
    }

    public void remove() {
        HOLDER.remove();
    }

    private Object claim(String key) {
        Map<String, Object> map = HOLDER.get();
        return map == null ? null : map.get(key);
    }

    private Integer toInt(Object value) {
        return value == null ? null : Integer.valueOf(value.toString());
    }
}
