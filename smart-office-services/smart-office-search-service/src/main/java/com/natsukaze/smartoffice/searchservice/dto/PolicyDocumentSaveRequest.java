package com.natsukaze.smartoffice.searchservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PolicyDocumentSaveRequest {

    @NotBlank
    private String title;

    private String content;

    private String summary;

    private String documentVersion;

    private String status;
}
