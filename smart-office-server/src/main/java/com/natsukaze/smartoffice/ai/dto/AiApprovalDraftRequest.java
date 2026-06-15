package com.natsukaze.smartoffice.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiApprovalDraftRequest {

    @NotBlank
    private String text;
}
