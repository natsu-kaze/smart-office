package com.natsukaze.smartoffice.approvalservice.dto;

import com.natsukaze.smartoffice.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalPageQuery extends PageQuery {

    private String approvalType;

    private String status;

    private String keyword;
}
