package com.workorder.aspect;

import cn.hutool.core.util.StrUtil;
import com.workorder.common.UserContext;
import com.workorder.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 幂等切面：
 * 1. 首次请求写入本地窗口；
 * 2. 窗口内重复请求直接返回统一提示；
 * 3. 业务执行失败时移除标记，允许用户修正后重新提交；
 * 4. 数据库 request_id 唯一索引作为多实例部署下的最终兜底。
 */
@Slf4j
@Aspect
@Component
public class IdempotentAspect {

    private static final int MAX_CACHE_SIZE = 5000;
    private static final Map<String, Long> CACHE = new ConcurrentHashMap<>();

    @Autowired
    private UserContext userContext;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = currentRequest();
        String requestId = request == null ? null : request.getHeader("X-Request-Id");
        if (StrUtil.isBlank(requestId)) {
            throw new CustomException("缺少幂等标识，请刷新页面后重试");
        }
        if (requestId.length() > 64) {
            throw new CustomException("幂等标识格式不正确");
        }

        String key = userContext.getUserId() + ":" + requestId;
        long now = System.currentTimeMillis();
        cleanup(now);

        Long previous = CACHE.putIfAbsent(key, now);
        if (previous != null && now - previous < idempotent.window()) {
            log.warn("重复提交被拦截，userId={}，requestId={}", userContext.getUserId(), requestId);
            throw new CustomException("请勿重复提交，请稍后再试");
        }

        try {
            return pjp.proceed();
        } catch (Throwable e) {
            CACHE.remove(key);
            throw e;
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private void cleanup(long now) {
        if (CACHE.size() < MAX_CACHE_SIZE) {
            return;
        }
        CACHE.entrySet().removeIf(entry -> now - entry.getValue() > 10 * 60 * 1000L);
    }
}
