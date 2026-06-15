package com.natsukaze.smartoffice.auth.controller;

import com.natsukaze.smartoffice.auth.dto.AuthUserVO;
import com.natsukaze.smartoffice.auth.dto.LoginRequest;
import com.natsukaze.smartoffice.auth.dto.LoginResponse;
import com.natsukaze.smartoffice.auth.security.UserPrincipal;
import com.natsukaze.smartoffice.auth.service.AuthService;
import com.natsukaze.smartoffice.common.core.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public Result<Void> logout() {
        return Result.success();
    }
}
