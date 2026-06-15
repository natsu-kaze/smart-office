package com.natsukaze.smartoffice.ai.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiApprovalDraftVO {

    private String approvalType;

    private String title;

    private String content;

    private String suggestion;
}
