package com.natsukaze.smartoffice.aiservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiApprovalDraftRequest {

    @NotBlank
    private String text;
}
