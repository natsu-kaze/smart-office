package com.natsukaze.smartoffice.searchservice.dto;

import com.natsukaze.smartoffice.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PolicyDocumentPageQuery extends PageQuery {

    private String keyword;

    private String status;
}
