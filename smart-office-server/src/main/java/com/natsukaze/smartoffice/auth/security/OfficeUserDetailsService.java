package com.natsukaze.smartoffice.auth.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.natsukaze.smartoffice.user.entity.SysRole;
import com.natsukaze.smartoffice.user.entity.SysUser;
import com.natsukaze.smartoffice.user.entity.SysUserRole;
import com.natsukaze.smartoffice.user.mapper.SysRoleMapper;
import com.natsukaze.smartoffice.user.mapper.SysUserMapper;
import com.natsukaze.smartoffice.user.mapper.SysUserRoleMapper;
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

    private final SysUserMapper sysUserMapper;

    private final SysUserRoleMapper sysUserRoleMapper;

    private final SysRoleMapper sysRoleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
        if (user == null) {
            throw new UsernameNotFoundException("user not found");
        }
        List<Long> roleIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, user.getId()))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
        List<SimpleGrantedAuthority> authorities;
        if (CollectionUtils.isEmpty(roleIds)) {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
        } else {
            authorities = sysRoleMapper.selectBatchIds(roleIds)
                    .stream()
                    .filter(role -> Integer.valueOf(1).equals(role.getStatus()))
                    .map(SysRole::getRoleCode)
                    .map(roleCode -> new SimpleGrantedAuthority("ROLE_" + roleCode))
                    .toList();
        }
        return new UserPrincipal(user, List.copyOf(authorities));
    }
}
