package com.natsukaze.smartoffice.systemservice.user.dto;

import com.natsukaze.smartoffice.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageQuery extends PageQuery {

    private String keyword;

    private Integer status;
}
