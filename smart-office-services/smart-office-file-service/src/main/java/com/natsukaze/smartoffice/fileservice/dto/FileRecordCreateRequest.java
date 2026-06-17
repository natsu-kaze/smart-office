package com.natsukaze.smartoffice.fileservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FileRecordCreateRequest {

    @NotBlank
    private String originalName;

    @NotBlank
    private String storageName;

    @NotBlank
    private String bucket;

    @NotBlank
    private String objectKey;

    private String contentType;

    private Long size;

    private String url;

    private String businessType;

    private Long businessId;
}
