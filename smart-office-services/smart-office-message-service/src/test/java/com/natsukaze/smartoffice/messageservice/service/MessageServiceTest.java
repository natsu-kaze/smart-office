package com.natsukaze.smartoffice.messageservice.service;

import com.natsukaze.smartoffice.api.message.dto.NoticeCreateCommand;
import com.natsukaze.smartoffice.common.enums.BusinessType;
import com.natsukaze.smartoffice.messageservice.entity.MessageNotice;
import com.natsukaze.smartoffice.messageservice.mapper.MessageNoticeMapper;
import com.natsukaze.smartoffice.messageservice.mapper.MessageTodoMapper;
import com.natsukaze.smartoffice.messageservice.mq.NoticeMessageProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageNoticeMapper noticeMapper;

    @Mock
    private MessageTodoMapper todoMapper;

    @Mock
    private NoticeMessageProducer noticeMessageProducer;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(noticeMapper, todoMapper, noticeMessageProducer);
    }

    @Test
    void createNoticeSendsToRabbitMqWhenAsyncEnabled() {
        NoticeCreateCommand command = noticeCommand();
        when(noticeMessageProducer.asyncEnabled()).thenReturn(true);

        messageService.createNotice(command);

        verify(noticeMessageProducer).send(command);
        verify(noticeMapper, never()).insert(org.mockito.ArgumentMatchers.any(MessageNotice.class));
    }

    @Test
    void createNoticeSavesDirectlyWhenAsyncDisabled() {
        NoticeCreateCommand command = noticeCommand();
        when(noticeMessageProducer.asyncEnabled()).thenReturn(false);

        messageService.createNotice(command);

        verify(noticeMessageProducer, never()).send(command);
        MessageNotice notice = captureInsertedNotice();
        assertThat(notice.getUserId()).isEqualTo(1001L);
        assertThat(notice.getTitle()).isEqualTo("Approval passed");
        assertThat(notice.getContent()).isEqualTo("Your approval was passed");
        assertThat(notice.getBusinessType()).isEqualTo(BusinessType.APPROVAL);
        assertThat(notice.getBusinessId()).isEqualTo(2002L);
        assertThat(notice.getReadStatus()).isZero();
    }

    @Test
    void createNoticeFallsBackToDirectSaveWhenRabbitMqFails() {
        NoticeCreateCommand command = noticeCommand();
        when(noticeMessageProducer.asyncEnabled()).thenReturn(true);
        org.mockito.Mockito.doThrow(new AmqpException("broker unavailable"))
                .when(noticeMessageProducer)
                .send(command);

        messageService.createNotice(command);

        MessageNotice notice = captureInsertedNotice();
        assertThat(notice.getUserId()).isEqualTo(1001L);
        assertThat(notice.getBusinessType()).isEqualTo(BusinessType.APPROVAL);
        assertThat(notice.getBusinessId()).isEqualTo(2002L);
    }

    @Test
    void saveNoticeIgnoresDuplicateBusinessNotice() {
        NoticeCreateCommand command = noticeCommand();
        when(noticeMapper.selectCount(any())).thenReturn(1L);

        messageService.saveNotice(command);

        verify(noticeMapper, never()).insert(any(MessageNotice.class));
    }

    private MessageNotice captureInsertedNotice() {
        ArgumentCaptor<MessageNotice> captor = ArgumentCaptor.forClass(MessageNotice.class);
        verify(noticeMapper).insert(captor.capture());
        return captor.getValue();
    }

    private NoticeCreateCommand noticeCommand() {
        return new NoticeCreateCommand(
                1001L,
                "Approval passed",
                "Your approval was passed",
                "APPROVAL",
                2002L
        );
    }
}
