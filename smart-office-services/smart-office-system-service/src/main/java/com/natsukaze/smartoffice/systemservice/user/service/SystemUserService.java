package com.natsukaze.smartoffice.systemservice.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.natsukaze.smartoffice.api.system.dto.CurrentUserDTO;
import com.natsukaze.smartoffice.api.system.dto.SystemAuthUserDTO;
import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import com.natsukaze.smartoffice.systemservice.user.dto.UserPageQuery;
import com.natsukaze.smartoffice.systemservice.user.entity.SysRole;
import com.natsukaze.smartoffice.systemservice.user.entity.SysUser;
import com.natsukaze.smartoffice.systemservice.user.entity.SysUserRole;
import com.natsukaze.smartoffice.systemservice.user.mapper.SysRoleMapper;
import com.natsukaze.smartoffice.systemservice.user.mapper.SysUserMapper;
import com.natsukaze.smartoffice.systemservice.user.mapper.SysUserRoleMapper;
import com.natsukaze.smartoffice.systemservice.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemUserService {

    private static final String DEFAULT_ROLE = "EMPLOYEE";

    private final SysUserMapper sysUserMapper;

    private final SysUserRoleMapper sysUserRoleMapper;

    private final SysRoleMapper sysRoleMapper;

    public PageResult<UserVO> page(UserPageQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(SysUser::getUsername, query.getKeyword())
                        .or()
                        .like(SysUser::getRealName, query.getKeyword())
                        .or()
                        .like(SysUser::getPhone, query.getKeyword()))
                .orderByDesc(SysUser::getCreateTime);
        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.from(page.convert(this::toVO));
    }

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

    private UserVO toVO(SysUser user) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, user.getId()));
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
        Map<Long, SysRole> roleMap = CollectionUtils.isEmpty(roleIds)
                ? Collections.emptyMap()
                : sysRoleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, Function.identity()));
        List<SysRole> roles = roleIds.stream()
                .map(roleMap::get)
                .filter(role -> role != null)
                .toList();
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .lastLoginTime(user.getLastLoginTime())
                .roleIds(roleIds)
                .roleCodes(roles.stream().map(SysRole::getRoleCode).toList())
                .roleNames(roles.stream().map(SysRole::getRoleName).toList())
                .build();
    }
}
