package com.natsukaze.smartoffice.approvalservice.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ApprovalRuleVO {

    private Long id;

    private String approvalType;

    private String name;

    private Integer priority;

    private BigDecimal amountLimit;

    private String applicantRoleCode;

    private Long deptId;

    private String requiredRoles;

    private Integer timeoutHours;

    private String timeoutAction;

    private Integer status;

    private String remark;
}
