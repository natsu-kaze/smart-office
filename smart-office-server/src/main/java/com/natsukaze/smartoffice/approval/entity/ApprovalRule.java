package com.natsukaze.smartoffice.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_rule")
public class ApprovalRule extends BaseEntity {

    private String approvalType;

    private BigDecimal amountLimit;

    private String requiredRoles;

    private Integer status;

    private String remark;
}
