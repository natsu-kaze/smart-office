package com.natsukaze.smartoffice.systemservice.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleSaveRequest {

    @NotBlank
    private String roleCode;

    @NotBlank
    private String roleName;

    private Integer sort = 0;

    private Integer status = 1;

    private String remark;
}
