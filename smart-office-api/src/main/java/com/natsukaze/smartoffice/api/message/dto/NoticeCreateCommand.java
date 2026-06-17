package com.natsukaze.smartoffice.api.message.dto;

public record NoticeCreateCommand(
        Long userId,
        String title,
        String content,
        String businessType,
        Long businessId
) {
}
