package com.natsukaze.smartoffice.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ApprovalAction implements BaseCodeEnum {

    CREATE("CREATE", "Create draft"),
    SUBMIT("SUBMIT", "Submit"),
    APPROVE("APPROVE", "Approve"),
    REJECT("REJECT", "Reject"),
    WITHDRAW("WITHDRAW", "Withdraw"),
    CLOSE("CLOSE", "Close"),
    DELETE("DELETE", "Delete");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    ApprovalAction(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
