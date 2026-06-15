package com.natsukaze.smartoffice.message.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageNoticeVO {

    private Long id;

    private String title;

    private String content;

    private String businessType;

    private Long businessId;

    private Integer readStatus;

    private LocalDateTime readTime;

    private LocalDateTime createTime;
}
