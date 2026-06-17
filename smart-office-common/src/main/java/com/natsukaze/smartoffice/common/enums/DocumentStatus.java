package com.natsukaze.smartoffice.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum DocumentStatus implements BaseCodeEnum {

    DRAFT("DRAFT", "Draft"),
    PUBLISHED("PUBLISHED", "Published"),
    ARCHIVED("ARCHIVED", "Archived");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    DocumentStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static DocumentStatus ofNullable(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(status -> status.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException("invalid document status"));
    }
}
