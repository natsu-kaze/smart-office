package com.natsukaze.smartoffice.common.exception;

import com.natsukaze.smartoffice.common.core.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(ErrorCode.BUSINESS_ERROR.getCode(), message);
    }

    public BusinessException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
