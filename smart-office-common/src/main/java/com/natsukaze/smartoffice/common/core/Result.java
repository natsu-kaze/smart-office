package com.natsukaze.smartoffice.common.core;

public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static Result<Void> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static Result<Void> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
