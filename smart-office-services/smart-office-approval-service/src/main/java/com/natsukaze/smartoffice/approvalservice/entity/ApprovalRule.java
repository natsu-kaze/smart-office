package com.natsukaze.smartoffice.approvalservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import com.natsukaze.smartoffice.common.enums.ApprovalType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_rule")
public class ApprovalRule extends BaseEntity {

    private ApprovalType approvalType;

    private BigDecimal amountLimit;

    private String requiredRoles;

    private Integer status;

    private String remark;
}
