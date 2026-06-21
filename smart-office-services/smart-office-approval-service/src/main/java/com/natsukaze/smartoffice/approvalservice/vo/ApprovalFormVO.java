package com.natsukaze.smartoffice.approvalservice.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ApprovalFormVO {

    private Long id;

    private String approvalType;

    private String title;

    private Long applicantUserId;

    private String applicantName;

    private Long applicantDeptId;

    private String applicantDeptName;

    private String content;

    private BigDecimal amount;

    private LocalDate leaveStartDate;

    private LocalDate leaveEndDate;

    private String status;

    private Long currentApproverId;

    private String currentApproverName;

    private LocalDateTime submittedAt;

    private LocalDateTime completedAt;

    private List<ApprovalRecordVO> records;
}
