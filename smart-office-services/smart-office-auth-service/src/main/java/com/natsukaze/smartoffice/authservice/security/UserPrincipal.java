package com.natsukaze.smartoffice.authservice.security;

import com.natsukaze.smartoffice.api.system.dto.SystemAuthUserDTO;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

    private final SystemAuthUserDTO user;

    private final List<GrantedAuthority> authorities;

    public UserPrincipal(SystemAuthUserDTO user, List<GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    public Long getUserId() {
        return user.userId();
    }

    public String getRealName() {
        return user.realName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.password();
    }

    @Override
    public String getUsername() {
        return user.username();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Integer.valueOf(1).equals(user.status());
    }
}
