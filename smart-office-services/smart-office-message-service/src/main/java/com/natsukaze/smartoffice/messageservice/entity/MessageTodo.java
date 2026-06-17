package com.natsukaze.smartoffice.messageservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import com.natsukaze.smartoffice.common.enums.BusinessType;
import com.natsukaze.smartoffice.common.enums.TodoStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message_todo")
public class MessageTodo extends BaseEntity {

    private Long userId;

    private String title;

    private BusinessType businessType;

    private Long businessId;

    private TodoStatus status;

    private LocalDateTime dueTime;

    private LocalDateTime completedTime;
}
