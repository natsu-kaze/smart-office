package com.natsukaze.smartoffice.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message_todo")
public class MessageTodo extends BaseEntity {

    private Long userId;

    private String title;

    private String businessType;

    private Long businessId;

    private String status;

    private LocalDateTime dueTime;

    private LocalDateTime completedTime;
}
