package com.workorder.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.workorder.common.JwtUtil;
import com.workorder.common.Result;
import com.workorder.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserContext userContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("token");
        if (StrUtil.isBlank(token)) {
            return writeUnauthorized(response);
        }
        try {
            Map<String, Object> claims = jwtUtil.parseToken(token);
            userContext.set(claims);
            return true;
        } catch (Exception e) {
            return writeUnauthorized(response);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        userContext.remove();
    }

    private boolean writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSONUtil.toJsonStr(
                Result.error(Result.UNAUTHORIZED, "登录已失效，请重新登录")));
        return false;
    }
}
