package com.natsukaze.smartoffice.orgservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentSaveRequest {

    private Long parentId = 0L;

    @NotBlank
    private String departmentCode;

    @NotBlank
    private String departmentName;

    private Long leaderUserId;

    private Integer sort;

    private Integer status;
}
