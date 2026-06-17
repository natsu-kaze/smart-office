package com.natsukaze.smartoffice.messageservice.dto;

import com.natsukaze.smartoffice.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TodoPageQuery extends PageQuery {

    private String status;

    private String businessType;
}
