package com.workorder.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等控制：请求头 X-Request-Id 在一次提交中唯一，重复请求在时间窗口内直接被拦截
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 幂等时间窗口（毫秒），默认 5 分钟 */
    long window() default 5 * 60 * 1000L;
}
