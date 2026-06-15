package com.natsukaze.smartoffice.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import com.natsukaze.smartoffice.common.enums.ApprovalAction;
import com.natsukaze.smartoffice.common.enums.ApprovalStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_record")
public class ApprovalRecord extends BaseEntity {

    private Long formId;

    private ApprovalAction action;

    private Long operatorUserId;

    private ApprovalStatus fromStatus;

    private ApprovalStatus toStatus;

    private String comment;
}
