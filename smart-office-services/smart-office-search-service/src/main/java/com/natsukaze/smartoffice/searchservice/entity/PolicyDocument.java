package com.natsukaze.smartoffice.searchservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import com.natsukaze.smartoffice.common.enums.DocumentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("policy_document")
public class PolicyDocument extends BaseEntity {

    private String title;

    private String content;

    private String summary;

    private String documentVersion;

    private DocumentStatus status;

    private Long publisherId;

    private LocalDateTime publishedAt;
}
