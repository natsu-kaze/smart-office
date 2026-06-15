package com.natsukaze.smartoffice.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ApprovalType implements BaseCodeEnum {

    LEAVE("LEAVE", "Leave approval"),
    OVERTIME("OVERTIME", "Overtime approval"),
    EXPENSE("EXPENSE", "Expense approval"),
    GENERAL("GENERAL", "General approval");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    ApprovalType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ApprovalType of(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException("invalid approval type"));
    }
}
