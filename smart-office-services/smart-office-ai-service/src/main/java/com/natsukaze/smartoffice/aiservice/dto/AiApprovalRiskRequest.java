package com.natsukaze.smartoffice.aiservice.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiApprovalRiskRequest {

    private String approvalType;

    private BigDecimal amount;

    private String content;
}
