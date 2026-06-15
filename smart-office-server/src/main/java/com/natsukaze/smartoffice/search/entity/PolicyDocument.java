package com.natsukaze.smartoffice.search.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
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

    private String status;

    private Long publisherId;

    private LocalDateTime publishedAt;
}
