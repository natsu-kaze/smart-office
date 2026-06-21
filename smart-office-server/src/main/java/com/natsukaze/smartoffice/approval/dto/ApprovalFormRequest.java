package com.natsukaze.smartoffice.approval.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ApprovalFormRequest {

    @NotBlank
    private String approvalType;

    @NotBlank
    private String title;

    private String content;

    private BigDecimal amount;

    private LocalDate leaveStartDate;

    private LocalDate leaveEndDate;
}
