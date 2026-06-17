package com.natsukaze.smartoffice.messageservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import com.natsukaze.smartoffice.common.enums.BusinessType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message_notice")
public class MessageNotice extends BaseEntity {

    private Long userId;

    private String title;

    private String content;

    private BusinessType businessType;

    private Long businessId;

    private Integer readStatus;

    private LocalDateTime readTime;
}
