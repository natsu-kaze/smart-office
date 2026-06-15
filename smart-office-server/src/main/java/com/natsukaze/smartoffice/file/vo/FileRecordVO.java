package com.natsukaze.smartoffice.file.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileRecordVO {

    private Long id;

    private String originalName;

    private String storageName;

    private String bucket;

    private String objectKey;

    private String contentType;

    private Long size;

    private String url;

    private Long uploaderId;

    private String businessType;

    private Long businessId;
}
