package com.workorder.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.workorder.common.UserContext;
import com.workorder.domain.pojo.OperateLog;
import com.workorder.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 操作日志切面：记录操作人、接口、参数、结果与耗时，日志写入失败不影响主流程
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    private static final int MAX_TEXT_LENGTH = 2000;

    @Autowired
    private LogService logService;

    @Autowired
    private UserContext userContext;

    @Around("@annotation(logRecord)")
    public Object recordLog(ProceedingJoinPoint pjp, LogRecord logRecord) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            try {
                saveLog(pjp, logRecord, start, result, error);
            } catch (Exception e) {
                log.warn("操作日志记录失败，不影响业务结果", e);
            }
        }
    }

    private void saveLog(ProceedingJoinPoint pjp, LogRecord logRecord,
                         long start, Object result, Throwable error) {
        Signature signature = pjp.getSignature();
        OperateLog operateLog = new OperateLog();
        operateLog.setOperateUserId(userContext.getUserId());
        operateLog.setDescription(logRecord.value());
        operateLog.setClassName(signature.getDeclaringTypeName());
        operateLog.setMethodName(signature.getName());
        operateLog.setMethodParams(truncate(toJson(pjp.getArgs())));
        operateLog.setReturnValue(error == null
                ? truncate(toJson(result))
                : truncate("异常：" + error.getMessage()));
        operateLog.setCostTime(System.currentTimeMillis() - start);
        operateLog.setOperateTime(LocalDateTime.now());
        logService.save(operateLog);
    }

    private String toJson(Object value) {
        try {
            return value == null ? null : JSONUtil.toJsonStr(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String truncate(String text) {
        if (StrUtil.isBlank(text) || text.length() <= MAX_TEXT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_TEXT_LENGTH);
    }
}
