package com.natsukaze.smartoffice.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import com.natsukaze.smartoffice.user.dto.AssignUserRolesRequest;
import com.natsukaze.smartoffice.user.dto.UserCreateRequest;
import com.natsukaze.smartoffice.user.dto.UserPageQuery;
import com.natsukaze.smartoffice.user.dto.UserStatusRequest;
import com.natsukaze.smartoffice.user.dto.UserUpdateRequest;
import com.natsukaze.smartoffice.user.entity.SysRole;
import com.natsukaze.smartoffice.user.entity.SysUser;
import com.natsukaze.smartoffice.user.entity.SysUserRole;
import com.natsukaze.smartoffice.user.mapper.SysRoleMapper;
import com.natsukaze.smartoffice.user.mapper.SysUserMapper;
import com.natsukaze.smartoffice.user.mapper.SysUserRoleMapper;
import com.natsukaze.smartoffice.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final SysUserMapper sysUserMapper;

    private final SysRoleMapper sysRoleMapper;

    private final SysUserRoleMapper sysUserRoleMapper;

    private final PasswordEncoder passwordEncoder;

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

    @Transactional
    public UserVO create(UserCreateRequest request) {
        ensureUsernameAvailable(request.getUsername(), null);
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setAvatar(request.getAvatar());
        user.setStatus(1);
        sysUserMapper.insert(user);
        return toVO(user);
    }

    @Transactional
    public UserVO update(Long id, UserUpdateRequest request) {
        SysUser user = requireUser(id);
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setAvatar(request.getAvatar());
        if (request.getStatus() != null) {
            ensureStatus(request.getStatus());
            user.setStatus(request.getStatus());
        }
        sysUserMapper.updateById(user);
        return toVO(requireUser(id));
    }

    @Transactional
    public void updateStatus(Long id, UserStatusRequest request) {
        ensureStatus(request.getStatus());
        SysUser user = requireUser(id);
        user.setStatus(request.getStatus());
        sysUserMapper.updateById(user);
    }

    @Transactional
    public void delete(Long id) {
        requireUser(id);
        sysUserRoleMapper.physicalDeleteByUserId(id);
        sysUserMapper.deleteById(id);
    }

    @Transactional
    public void assignRoles(Long id, AssignUserRolesRequest request) {
        requireUser(id);
        List<Long> roleIds = request.getRoleIds().stream().distinct().toList();
        ensureRolesExist(roleIds);
        sysUserRoleMapper.physicalDeleteByUserId(id);
        roleIds.forEach(roleId -> {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(id);
            relation.setRoleId(roleId);
            sysUserRoleMapper.insert(relation);
        });
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("user not found");
        }
        return user;
    }

    private void ensureUsernameAvailable(String username, Long excludeId) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .ne(excludeId != null, SysUser::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("username already exists");
        }
    }

    private void ensureRolesExist(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BusinessException("role not found");
        }
    }

    private void ensureStatus(Integer status) {
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException("status must be 0 or 1");
        }
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
