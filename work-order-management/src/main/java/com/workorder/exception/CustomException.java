package com.workorder.exception;

/**
 * 业务异常：message 会原样返回给前端，用于提示用户
 */
public class CustomException extends RuntimeException {

    public CustomException() {
    }

    public CustomException(String message) {
        super(message);
    }
}
