package com.natsukaze.smartoffice.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum TodoStatus implements BaseCodeEnum {

    PENDING("PENDING", "Pending"),
    DONE("DONE", "Done"),
    CANCELED("CANCELED", "Canceled");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    TodoStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TodoStatus ofNullable(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(status -> status.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException("invalid todo status"));
    }
}
