package com.natsukaze.smartoffice.approvalservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApprovalRuleRequest {

    @NotBlank
    private String approvalType;

    private String name;

    private Integer priority;

    private BigDecimal amountLimit;

    private String applicantRoleCode;

    private Long deptId;

    @NotBlank
    private String requiredRoles;

    private Integer timeoutHours;

    private String timeoutAction;

    private Integer status;

    private String remark;
}
