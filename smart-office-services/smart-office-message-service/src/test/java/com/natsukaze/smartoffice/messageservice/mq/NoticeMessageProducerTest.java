package com.natsukaze.smartoffice.messageservice.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.natsukaze.smartoffice.api.message.dto.NoticeCreateCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoticeMessageProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void sendRetriesWhenRabbitMqDeliveryFails() {
        MessageMqProperties properties = new MessageMqProperties();
        properties.setNoticeSendMaxAttempts(2);
        properties.setNoticeSendRetryIntervalMillis(0);
        NoticeMessageProducer producer = new NoticeMessageProducer(rabbitTemplate, new ObjectMapper(), properties);
        NoticeCreateCommand command = new NoticeCreateCommand(
                1001L,
                "Attendance abnormal",
                "missing check-out",
                "ATTENDANCE",
                2002L
        );
        doThrow(new AmqpException("temporary broker failure"))
                .doNothing()
                .when(rabbitTemplate)
                .convertAndSend(eq(properties.getNoticeExchange()),
                        eq(properties.getNoticeRoutingKey()),
                        anyString(),
                        any(MessagePostProcessor.class));

        producer.send(command);

        verify(rabbitTemplate, times(2)).convertAndSend(eq(properties.getNoticeExchange()),
                eq(properties.getNoticeRoutingKey()),
                anyString(),
                any(MessagePostProcessor.class));
    }
}
