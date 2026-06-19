package com.natsukaze.smartoffice.messageservice.mq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "smart-office.message.mq")
public class MessageMqProperties {

    private boolean asyncNotificationEnabled = true;

    private String noticeExchange = "smart-office.message.exchange";

    private String noticeQueue = "smart-office.message.notice.queue";

    private String noticeRoutingKey = "notice.created";

    private int noticeSendMaxAttempts = 3;

    private long noticeSendRetryIntervalMillis = 200;
}
