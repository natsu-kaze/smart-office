package com.natsukaze.smartoffice.messageservice.mq;

import com.natsukaze.smartoffice.api.message.dto.NoticeCreateCommand;

public record NoticeCreateEvent(
        Long userId,
        String title,
        String content,
        String businessType,
        Long businessId
) {

    public static NoticeCreateEvent from(NoticeCreateCommand command) {
        return new NoticeCreateEvent(
                command.userId(),
                command.title(),
                command.content(),
                command.businessType(),
                command.businessId()
        );
    }

    public NoticeCreateCommand toCommand() {
        return new NoticeCreateCommand(userId, title, content, businessType, businessId);
    }
}
