package com.natsukaze.smartoffice.messageservice.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "smart-office.message.mq", name = "async-notification-enabled", havingValue = "true", matchIfMissing = true)
public class MessageRabbitConfig {

    private final MessageMqProperties properties;

    @Bean
    public DirectExchange noticeExchange() {
        return new DirectExchange(properties.getNoticeExchange(), true, false);
    }

    @Bean
    public Queue noticeQueue() {
        return new Queue(properties.getNoticeQueue(), true);
    }

    @Bean
    public Binding noticeBinding(DirectExchange noticeExchange, Queue noticeQueue) {
        return BindingBuilder.bind(noticeQueue)
                .to(noticeExchange)
                .with(properties.getNoticeRoutingKey());
    }
}
