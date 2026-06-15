package com.natsukaze.smartoffice.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_process")
public class ApprovalProcess extends BaseEntity {

    private Long formId;

    private Long approverUserId;

    private Integer stepOrder;

    private String status;

    private LocalDateTime approvedAt;

    private String comment;
}
