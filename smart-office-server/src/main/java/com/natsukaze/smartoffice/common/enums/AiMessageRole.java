package com.natsukaze.smartoffice.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum AiMessageRole implements BaseCodeEnum {

    USER("USER", "User"),
    ASSISTANT("ASSISTANT", "Assistant"),
    SYSTEM("SYSTEM", "System");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    AiMessageRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
