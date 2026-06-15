package com.natsukaze.smartoffice.auth.service;

import com.natsukaze.smartoffice.auth.dto.AuthUserVO;
import com.natsukaze.smartoffice.auth.dto.LoginRequest;
import com.natsukaze.smartoffice.auth.dto.LoginResponse;
import com.natsukaze.smartoffice.auth.security.JwtProperties;
import com.natsukaze.smartoffice.auth.security.JwtService;
import com.natsukaze.smartoffice.auth.security.UserPrincipal;
import com.natsukaze.smartoffice.user.entity.SysUser;
import com.natsukaze.smartoffice.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final JwtProperties jwtProperties;

    private final SysUserMapper sysUserMapper;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        SysUser user = principal.getUser();
        user.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return LoginResponse.builder()
                .token(jwtService.generateToken(principal))
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpiration())
                .user(toUserVO(principal))
                .build();
    }

    public AuthUserVO currentUser(UserPrincipal principal) {
        return toUserVO(principal);
    }

    private AuthUserVO toUserVO(UserPrincipal principal) {
        SysUser user = principal.getUser();
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .toList();
        return AuthUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .roles(roles)
                .build();
    }
}
