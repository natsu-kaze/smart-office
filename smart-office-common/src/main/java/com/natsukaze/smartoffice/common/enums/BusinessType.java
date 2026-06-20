package com.natsukaze.smartoffice.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum BusinessType implements BaseCodeEnum {

    APPROVAL("APPROVAL", "Approval"),
    ATTENDANCE("ATTENDANCE", "Attendance"),
    FILE("FILE", "File"),
    POLICY("POLICY", "Policy document"),
    ANNOUNCEMENT("ANNOUNCEMENT", "Announcement"),
    AI("AI", "AI assistant"),
    TEST("TEST", "Test"),
    GENERAL("GENERAL", "General");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    BusinessType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static BusinessType ofNullable(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException("invalid business type"));
    }
}
