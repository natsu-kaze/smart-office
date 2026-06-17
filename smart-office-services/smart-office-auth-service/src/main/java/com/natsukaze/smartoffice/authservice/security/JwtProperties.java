package com.natsukaze.smartoffice.authservice.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "smart-office.jwt")
public class JwtProperties {

    private String issuer = "smart-office";

    private String secret;

    private Long expiration = 86_400_000L;
}
