package com.natsukaze.smartoffice.attendanceservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "smart-office.xxl-job")
public class XxlJobProperties {

    private boolean enabled = false;

    private String adminAddresses = "http://127.0.0.1:8088/xxl-job-admin";

    private String appName = "smart-office-job-executor";

    private String address;

    private String ip;

    private int port = 9999;

    private String accessToken = "smart-office";

    private String logPath = "logs/xxl-job";

    private int logRetentionDays = 30;
}
