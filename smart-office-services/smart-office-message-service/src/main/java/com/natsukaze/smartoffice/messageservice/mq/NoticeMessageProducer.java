package com.natsukaze.smartoffice.messageservice.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.natsukaze.smartoffice.api.message.dto.NoticeCreateCommand;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

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
        } catch (JsonProcessingException ex) {
            throw new BusinessException("notice message serialization failed");
        }
    }
}
