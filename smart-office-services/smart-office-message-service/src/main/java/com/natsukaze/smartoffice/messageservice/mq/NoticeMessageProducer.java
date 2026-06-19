package com.natsukaze.smartoffice.messageservice.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.natsukaze.smartoffice.api.message.dto.NoticeCreateCommand;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper;

    private final MessageMqProperties properties;

    public boolean asyncEnabled() {
        return properties.isAsyncNotificationEnabled();
    }

    public void send(NoticeCreateCommand command) {
        try {
            String payload = objectMapper.writeValueAsString(NoticeCreateEvent.from(command));
            sendWithRetry(command, payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("notice message serialization failed");
        }
    }

    private void sendWithRetry(NoticeCreateCommand command, String payload) {
        int maxAttempts = Math.max(1, properties.getNoticeSendMaxAttempts());
        AmqpException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                rabbitTemplate.convertAndSend(
                        properties.getNoticeExchange(),
                        properties.getNoticeRoutingKey(),
                        payload,
                        message -> {
                            message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            return message;
                        }
                );
                return;
            } catch (AmqpException ex) {
                lastException = ex;
                if (attempt < maxAttempts) {
                    log.warn("RabbitMQ notice delivery failed, retrying. attempt={}/{}, userId={}, businessType={}, businessId={}, reason={}",
                            attempt, maxAttempts, command.userId(), command.businessType(), command.businessId(), ex.getMessage());
                    sleepBeforeRetry();
                }
            }
        }
        throw lastException;
    }

    private void sleepBeforeRetry() {
        long interval = properties.getNoticeSendRetryIntervalMillis();
        if (interval <= 0) {
            return;
        }
        try {
            Thread.sleep(interval);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("notice message retry interrupted");
        }
    }
}
