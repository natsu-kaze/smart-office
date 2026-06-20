package com.natsukaze.smartoffice.aiservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import com.natsukaze.smartoffice.common.enums.AiMessageRole;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_message")
public class AiMessage extends BaseEntity {

    private Long conversationId;

    private AiMessageRole role;

    private String content;

    private Integer tokens;
}
