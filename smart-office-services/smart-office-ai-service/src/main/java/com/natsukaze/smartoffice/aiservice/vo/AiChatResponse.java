package com.natsukaze.smartoffice.aiservice.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiChatResponse {

    private Long conversationId;

    private String answer;

    private Boolean degraded;
}
