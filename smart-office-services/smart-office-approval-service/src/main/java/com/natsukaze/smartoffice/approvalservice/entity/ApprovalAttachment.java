package com.natsukaze.smartoffice.approvalservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_attachment")
public class ApprovalAttachment extends BaseEntity {

    private Long formId;

    private Long fileId;
}
