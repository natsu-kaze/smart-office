package com.natsukaze.smartoffice.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleUpdateRequest {

    @NotBlank
    private String roleName;

    private Integer sort;

    private Integer status;

    private String remark;
}
