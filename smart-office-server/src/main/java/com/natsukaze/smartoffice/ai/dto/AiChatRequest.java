package com.natsukaze.smartoffice.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatRequest {

    private Long conversationId;

    @NotBlank
    private String message;
}
