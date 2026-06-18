package com.natsukaze.smartoffice.messageservice.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.natsukaze.smartoffice.messageservice.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smart-office.message.mq", name = "async-notification-enabled", havingValue = "true", matchIfMissing = true)
public class NoticeMessageConsumer {

    private final ObjectMapper objectMapper;

    private final MessageService messageService;

    @RabbitListener(queues = "${smart-office.message.mq.notice-queue:smart-office.message.notice.queue}")
    public void handleNotice(String payload) {
        NoticeCreateEvent event;
        try {
            event = objectMapper.readValue(payload, NoticeCreateEvent.class);
        } catch (JsonProcessingException ex) {
            log.error("Invalid notice message payload: {}", payload, ex);
            return;
        }
        messageService.saveNotice(event.toCommand());
    }
}
