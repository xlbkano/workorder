package com.workorder.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回结构：{ code, msg, data, timestamp }
 * code：1 成功；0 业务失败；400 参数错误；401 未登录；409 重复提交/数据冲突；500 系统异常
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {

    public static final int SUCCESS = 1;
    public static final int FAIL = 0;
    public static final int PARAM_ERROR = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int DUPLICATE = 409;
    public static final int SYSTEM_ERROR = 500;

    private int code;
    private String msg;
    private Object data;
    private long timestamp;

    private static Result of(int code, String msg, Object data) {
        return new Result(code, msg, data, System.currentTimeMillis());
    }

    public static Result success() {
        return of(SUCCESS, "success", null);
    }

    public static Result success(Object data) {
        return of(SUCCESS, "success", data);
    }

    public static Result success(String msg, Object data) {
        return of(SUCCESS, msg, data);
    }

    public static Result error(String msg) {
        return of(FAIL, msg, null);
    }

    public static Result error(int code, String msg) {
        return of(code, msg, null);
    }
}
