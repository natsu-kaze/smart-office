package com.natsukaze.smartoffice.searchservice.dto;

import lombok.Data;

@Data
public class PolicyDocumentUploadRequest {

    private String title;

    private String documentVersion;

    private String status;
}
