package com.natsukaze.smartoffice.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_form")
public class ApprovalForm extends BaseEntity {

    private String approvalType;

    private String title;

    private Long applicantUserId;

    private Long applicantDeptId;

    private String content;

    private BigDecimal amount;

    private String status;

    private Long currentApproverId;

    private LocalDateTime submittedAt;

    private LocalDateTime completedAt;
}
