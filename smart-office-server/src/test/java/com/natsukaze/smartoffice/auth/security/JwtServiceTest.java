package com.natsukaze.smartoffice.auth.security;

import com.natsukaze.smartoffice.user.entity.SysUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void generateTokenAndValidate() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("smart-office-test");
        properties.setSecret("smart-office-test-secret-at-least-32-bytes");
        properties.setExpiration(60_000L);
        JwtService jwtService = new JwtService(properties);

        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setRealName("System Admin");
        user.setPassword("{noop}123456");
        user.setStatus(1);
        UserPrincipal principal = new UserPrincipal(user, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        String token = jwtService.generateToken(principal);

        assertThat(jwtService.getUsername(token)).isEqualTo("admin");
        assertThat(jwtService.isTokenValid(token, principal)).isTrue();
    }
}
