package com.natsukaze.smartoffice.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AttendanceStatus implements BaseCodeEnum {

    NORMAL("NORMAL", "Normal"),
    LATE("LATE", "Late"),
    EARLY_LEAVE("EARLY_LEAVE", "Early leave"),
    MISSING("MISSING", "Missing punch"),
    LEAVE("LEAVE", "Leave"),
    OVERTIME("OVERTIME", "Overtime"),
    ABNORMAL("ABNORMAL", "Abnormal");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    AttendanceStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
