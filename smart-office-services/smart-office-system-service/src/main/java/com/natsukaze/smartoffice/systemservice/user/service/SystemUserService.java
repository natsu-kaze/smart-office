package com.natsukaze.smartoffice.systemservice.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.natsukaze.smartoffice.api.system.dto.CurrentUserDTO;
import com.natsukaze.smartoffice.api.system.dto.SystemAuthUserDTO;
import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import com.natsukaze.smartoffice.systemservice.user.entity.SysRole;
import com.natsukaze.smartoffice.systemservice.user.entity.SysUser;
import com.natsukaze.smartoffice.systemservice.user.entity.SysUserRole;
import com.natsukaze.smartoffice.systemservice.user.mapper.SysRoleMapper;
import com.natsukaze.smartoffice.systemservice.user.mapper.SysUserMapper;
import com.natsukaze.smartoffice.systemservice.user.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemUserService {

    private static final String DEFAULT_ROLE = "EMPLOYEE";

    private final SysUserMapper sysUserMapper;

    private final SysUserRoleMapper sysUserRoleMapper;

    private final SysRoleMapper sysRoleMapper;

    public CurrentUserDTO getCurrentUser(Long userId) {
        SysUser user = getRequiredUser(userId);
        return new CurrentUserDTO(user.getId(), user.getUsername(), user.getRealName(), null, null);
    }

    public SystemAuthUserDTO getAuthUserByUsername(String username) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
        return new SystemAuthUserDTO(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRealName(),
                user.getPhone(),
                user.getEmail(),
                user.getAvatar(),
                user.getStatus(),
                listRoleCodes(user.getId()));
    }

    public CurrentUserDTO getFirstUserByRole(String roleCode) {
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode)
                .last("LIMIT 1"));
        if (role == null) {
            return null;
        }
        SysUserRole relation = sysUserRoleMapper.selectOne(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, role.getId())
                .last("LIMIT 1"));
        return relation == null ? null : getCurrentUser(relation.getUserId());
    }

    @Transactional
    public void updateLastLoginTime(Long userId) {
        SysUser user = getRequiredUser(userId);
        user.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    private SysUser getRequiredUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
        return user;
    }

    private List<String> listRoleCodes(Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of(DEFAULT_ROLE);
        }
        List<String> roles = sysRoleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> Integer.valueOf(1).equals(role.getStatus()))
                .map(SysRole::getRoleCode)
                .toList();
        return roles.isEmpty() ? List.of(DEFAULT_ROLE) : roles;
    }
}
