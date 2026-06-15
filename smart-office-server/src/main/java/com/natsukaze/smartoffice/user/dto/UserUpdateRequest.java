package com.natsukaze.smartoffice.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @NotBlank
    private String realName;

    private String phone;

    @Email
    private String email;

    private String avatar;

    private Integer status;
}
