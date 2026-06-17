package com.natsukaze.smartoffice.approvalservice.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApprovalRecordVO {

    private Long id;

    private Long formId;

    private String action;

    private Long operatorUserId;

    private String operatorName;

    private String fromStatus;

    private String toStatus;

    private String comment;

    private LocalDateTime createTime;
}
