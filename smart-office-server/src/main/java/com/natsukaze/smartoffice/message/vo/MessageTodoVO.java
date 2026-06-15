package com.natsukaze.smartoffice.message.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageTodoVO {

    private Long id;

    private String title;

    private String businessType;

    private Long businessId;

    private String status;

    private LocalDateTime dueTime;

    private LocalDateTime completedTime;

    private LocalDateTime createTime;
}
