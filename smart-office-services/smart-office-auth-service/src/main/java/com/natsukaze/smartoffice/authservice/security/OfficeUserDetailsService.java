package com.natsukaze.smartoffice.authservice.security;

import com.natsukaze.smartoffice.api.system.client.SystemUserClient;
import com.natsukaze.smartoffice.api.system.dto.SystemAuthUserDTO;
import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.core.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OfficeUserDetailsService implements UserDetailsService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final SystemUserClient systemUserClient;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Result<SystemAuthUserDTO> result = systemUserClient.getByUsername(username);
        if (result.code() != ErrorCode.SUCCESS.getCode() || result.data() == null) {
            throw new UsernameNotFoundException("user not found");
        }
        SystemAuthUserDTO user = result.data();
        List<SimpleGrantedAuthority> authorities = CollectionUtils.isEmpty(user.roles())
                ? List.of(new SimpleGrantedAuthority(ROLE_PREFIX + "EMPLOYEE"))
                : user.roles().stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .toList();
        return new UserPrincipal(user, List.copyOf(authorities));
    }
}
