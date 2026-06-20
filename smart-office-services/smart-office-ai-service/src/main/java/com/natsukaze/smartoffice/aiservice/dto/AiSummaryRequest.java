package com.natsukaze.smartoffice.aiservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiSummaryRequest {

    @NotBlank
    private String content;
}
