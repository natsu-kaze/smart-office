package com.natsukaze.smartoffice.org.dto;

import com.natsukaze.smartoffice.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PositionPageQuery extends PageQuery {

    private String keyword;

    private Long departmentId;

    private Integer status;
}
