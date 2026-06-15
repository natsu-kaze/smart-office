package com.natsukaze.smartoffice.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import com.natsukaze.smartoffice.common.enums.ApprovalStatus;
import com.natsukaze.smartoffice.common.enums.ApprovalType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_form")
public class ApprovalForm extends BaseEntity {

    private ApprovalType approvalType;

    private String title;

    private Long applicantUserId;

    private Long applicantDeptId;

    private String content;

    private BigDecimal amount;

    private ApprovalStatus status;

    private Long currentApproverId;

    private LocalDateTime submittedAt;

    private LocalDateTime completedAt;
}
