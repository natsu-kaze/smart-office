package com.natsukaze.smartoffice.fileservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "smart-office.minio")
public class MinioProperties {

    private String endpoint = "http://127.0.0.1:9000";

    private String accessKey = "smartoffice";

    private String secretKey = "smartoffice123456";

    private String bucket = "smart-office-files";

    private int previewExpiryMinutes = 30;

    private long maxSizeMb = 20;

    private String allowedContentTypes = "application/pdf,image/png,image/jpeg,text/plain,text/markdown,application/vnd.openxmlformats-officedocument.wordprocessingml.document";
}
