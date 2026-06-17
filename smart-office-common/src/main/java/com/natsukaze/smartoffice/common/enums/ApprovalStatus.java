package com.natsukaze.smartoffice.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ApprovalStatus implements BaseCodeEnum {

    DRAFT("DRAFT", "Draft"),
    PENDING("PENDING", "Pending approval"),
    PROCESSING("PROCESSING", "Processing"),
    APPROVED("APPROVED", "Approved"),
    REJECTED("REJECTED", "Rejected"),
    WITHDRAWN("WITHDRAWN", "Withdrawn"),
    CLOSED("CLOSED", "Closed");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    ApprovalStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ApprovalStatus of(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException("invalid approval status"));
    }
}
