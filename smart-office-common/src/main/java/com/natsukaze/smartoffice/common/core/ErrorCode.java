package com.natsukaze.smartoffice.common.core;

public enum ErrorCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或登录已过期"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
