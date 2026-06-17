package com.natsukaze.smartoffice.messageservice.dto;

import com.natsukaze.smartoffice.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MessagePageQuery extends PageQuery {

    private Integer readStatus;

    private String businessType;
}
