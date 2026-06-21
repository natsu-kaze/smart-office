package com.natsukaze.smartoffice.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum TimeoutAction implements BaseCodeEnum {

    AUTO_APPROVE("AUTO_APPROVE", "Auto-approve on timeout"),
    AUTO_REJECT("AUTO_REJECT", "Auto-reject on timeout"),
    ESCALATE("ESCALATE", "Escalate to next approver on timeout");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    TimeoutAction(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TimeoutAction of(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(a -> a.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException("invalid timeout action: " + code));
    }

    public static TimeoutAction ofNullable(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(a -> a.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
