package com.natsukaze.smartoffice.authservice.controller;

import com.natsukaze.smartoffice.authservice.dto.AuthUserVO;
import com.natsukaze.smartoffice.authservice.dto.LoginRequest;
import com.natsukaze.smartoffice.authservice.dto.LoginResponse;
import com.natsukaze.smartoffice.authservice.security.UserPrincipal;
import com.natsukaze.smartoffice.authservice.service.AuthService;
import com.natsukaze.smartoffice.common.core.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<AuthUserVO> me(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(authService.currentUser(principal));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(authorization);
        return Result.success();
    }
}
