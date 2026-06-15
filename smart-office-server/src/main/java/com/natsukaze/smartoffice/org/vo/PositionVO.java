package com.natsukaze.smartoffice.org.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PositionVO {

    private Long id;

    private Long departmentId;

    private String departmentName;

    private String positionCode;

    private String positionName;

    private Integer sort;

    private Integer status;
}
