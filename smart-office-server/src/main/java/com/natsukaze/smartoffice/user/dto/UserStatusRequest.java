package com.natsukaze.smartoffice.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusRequest {

    @NotNull
    private Integer status;
}
