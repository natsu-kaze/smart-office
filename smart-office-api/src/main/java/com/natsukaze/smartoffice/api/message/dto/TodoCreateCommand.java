package com.natsukaze.smartoffice.api.message.dto;

import java.time.LocalDateTime;

public record TodoCreateCommand(
        Long userId,
        String title,
        String businessType,
        Long businessId,
        LocalDateTime dueTime
) {
}
