package com.natsukaze.smartoffice.org.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PositionSaveRequest {

    private Long departmentId;

    @NotBlank
    private String positionCode;

    @NotBlank
    private String positionName;

    private Integer sort;

    private Integer status;
}
