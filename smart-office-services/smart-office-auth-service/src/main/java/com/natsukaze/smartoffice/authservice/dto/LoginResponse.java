package com.natsukaze.smartoffice.authservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String token;

    private String tokenType;

    private Long expiresIn;

    private AuthUserVO user;
}
