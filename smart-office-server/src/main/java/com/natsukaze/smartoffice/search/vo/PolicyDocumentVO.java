package com.natsukaze.smartoffice.search.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PolicyDocumentVO {

    private Long id;

    private String title;

    private String content;

    private String summary;

    private String documentVersion;

    private String status;

    private Long publisherId;

    private LocalDateTime publishedAt;
}
