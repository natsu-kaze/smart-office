package com.natsukaze.smartoffice.auth.controller;

import com.natsukaze.smartoffice.auth.dto.AuthUserVO;
import com.natsukaze.smartoffice.auth.dto.LoginRequest;
import com.natsukaze.smartoffice.auth.dto.LoginResponse;
import com.natsukaze.smartoffice.auth.security.UserPrincipal;
import com.natsukaze.smartoffice.auth.service.AuthService;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.user.entity.SysUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);

    private final AuthController authController = new AuthController(authService);

    @Test
    void loginReturnsTokenAndUser() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");
        AuthUserVO user = AuthUserVO.builder()
                .id(1L)
                .username("admin")
                .realName("System Admin")
                .roles(List.of("ADMIN"))
                .build();
        LoginResponse response = LoginResponse.builder()
                .token("token")
                .tokenType("Bearer")
                .expiresIn(60_000L)
                .user(user)
                .build();
        when(authService.login(request)).thenReturn(response);

        Result<LoginResponse> result = authController.login(request);

        assertThat(result.getCode()).isZero();
        assertThat(result.getData().getToken()).isEqualTo("token");
        assertThat(result.getData().getUser().getUsername()).isEqualTo("admin");
    }

    @Test
    void meReturnsCurrentUser() {
        SysUser sysUser = new SysUser();
        sysUser.setId(1L);
        sysUser.setUsername("admin");
        sysUser.setRealName("System Admin");
        UserPrincipal principal = new UserPrincipal(sysUser, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        AuthUserVO user = AuthUserVO.builder()
                .id(1L)
                .username("admin")
                .roles(List.of("ADMIN"))
                .build();
        when(authService.currentUser(principal)).thenReturn(user);

        Result<AuthUserVO> result = authController.me(principal);

        assertThat(result.getCode()).isZero();
        assertThat(result.getData().getRoles()).containsExactly("ADMIN");
    }

    @Test
    void logoutReturnsSuccess() {
        assertThat(authController.logout().getCode()).isZero();
    }
}
