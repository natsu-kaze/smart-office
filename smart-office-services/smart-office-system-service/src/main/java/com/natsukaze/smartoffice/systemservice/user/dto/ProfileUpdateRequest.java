package com.natsukaze.smartoffice.systemservice.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileUpdateRequest {

    @NotBlank
    private String realName;

    private String phone;

    @Email
    private String email;

    private String avatar;
}
