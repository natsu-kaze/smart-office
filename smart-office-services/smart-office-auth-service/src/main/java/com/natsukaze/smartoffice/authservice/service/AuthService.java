package com.natsukaze.smartoffice.authservice.service;

import com.natsukaze.smartoffice.api.system.client.SystemUserClient;
import com.natsukaze.smartoffice.api.system.dto.SystemAuthUserDTO;
import com.natsukaze.smartoffice.authservice.dto.AuthUserVO;
import com.natsukaze.smartoffice.authservice.dto.LoginRequest;
import com.natsukaze.smartoffice.authservice.dto.LoginResponse;
import com.natsukaze.smartoffice.authservice.security.JwtProperties;
import com.natsukaze.smartoffice.authservice.security.JwtService;
import com.natsukaze.smartoffice.authservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final JwtProperties jwtProperties;

    private final SystemUserClient systemUserClient;

    private final AuthTokenStore tokenStore;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);
        tokenStore.store(token, principal, jwtProperties.getExpiration());
        systemUserClient.updateLastLoginTime(principal.getUserId());
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpiration())
                .user(toUserVO(principal))
                .build();
    }

    public AuthUserVO currentUser(UserPrincipal principal) {
        return toUserVO(principal);
    }

    public void logout(String authorization) {
        tokenStore.revoke(resolveToken(authorization));
    }

    private String resolveToken(String authorization) {
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length());
        }
        return null;
    }

    private AuthUserVO toUserVO(UserPrincipal principal) {
        SystemAuthUserDTO user = principal.getUser();
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .toList();
        return AuthUserVO.builder()
                .id(user.userId())
                .username(user.username())
                .realName(user.realName())
                .phone(user.phone())
                .email(user.email())
                .avatar(user.avatar())
                .roles(roles)
                .build();
    }
}
