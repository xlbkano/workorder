package com.workorder.exception;

import com.workorder.common.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常捕获：把各类异常统一转换为标准返回体，避免堆栈直接暴露给前端
 */
@Slf4j
@RestControllerAdvice
public class GlobalException {

    /** 业务校验失败 */
    @ExceptionHandler(CustomException.class)
    public Result handleCustomException(CustomException e) {
        return Result.error(e.getMessage());
    }

    /** @RequestBody 参数校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError == null ? "请求参数不合法" : fieldError.getDefaultMessage();
        return Result.error(Result.PARAM_ERROR, msg);
    }

    /** GET 查询参数绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public Result handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError == null ? "请求参数不合法" : fieldError.getDefaultMessage();
        return Result.error(Result.PARAM_ERROR, msg);
    }

    /** 方法参数约束校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        return Result.error(Result.PARAM_ERROR, msg);
    }

    /** 请求体 JSON 解析失败（格式错误、类型不匹配等） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());
        return Result.error(Result.PARAM_ERROR, "请求参数格式错误，请检查后重试");
    }

    /** 缺少必填请求参数 / 参数类型转换失败 */
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public Result handleBadRequest(Exception e) {
        log.warn("请求参数异常：{}", e.getMessage());
        return Result.error(Result.PARAM_ERROR, "请求参数缺失或格式不正确");
    }

    /** 唯一索引冲突（幂等兜底 / 工单号冲突） */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一键冲突：{}", e.getMessage());
        return Result.error(Result.DUPLICATE, "请求已处理，请勿重复提交");
    }

    /** 兜底异常：记录日志，返回标准化提示，不暴露内部细节 */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(Result.SYSTEM_ERROR, "系统繁忙，请稍后重试");
    }
}
