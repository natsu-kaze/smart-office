package com.natsukaze.smartoffice.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import com.natsukaze.smartoffice.user.dto.AssignRoleMenusRequest;
import com.natsukaze.smartoffice.user.dto.RoleCreateRequest;
import com.natsukaze.smartoffice.user.dto.RolePageQuery;
import com.natsukaze.smartoffice.user.dto.RoleUpdateRequest;
import com.natsukaze.smartoffice.user.entity.SysMenu;
import com.natsukaze.smartoffice.user.entity.SysRole;
import com.natsukaze.smartoffice.user.entity.SysRoleMenu;
import com.natsukaze.smartoffice.user.mapper.SysMenuMapper;
import com.natsukaze.smartoffice.user.mapper.SysRoleMapper;
import com.natsukaze.smartoffice.user.mapper.SysRoleMenuMapper;
import com.natsukaze.smartoffice.user.mapper.SysUserRoleMapper;
import com.natsukaze.smartoffice.user.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleManagementService {

    private final SysRoleMapper sysRoleMapper;

    private final SysMenuMapper sysMenuMapper;

    private final SysRoleMenuMapper sysRoleMenuMapper;

    private final SysUserRoleMapper sysUserRoleMapper;

    public PageResult<RoleVO> page(RolePageQuery query) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(SysRole::getRoleCode, query.getKeyword())
                        .or()
                        .like(SysRole::getRoleName, query.getKeyword()))
                .orderByAsc(SysRole::getSort)
                .orderByDesc(SysRole::getCreateTime);
        Page<SysRole> page = sysRoleMapper.selectPage(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.from(page.convert(this::toVO));
    }

    @Transactional
    public RoleVO create(RoleCreateRequest request) {
        String roleCode = normalizeRoleCode(request.getRoleCode());
        ensureRoleCodeAvailable(roleCode);
        SysRole role = new SysRole();
        role.setRoleCode(roleCode);
        role.setRoleName(request.getRoleName());
        role.setSort(request.getSort() == null ? 0 : request.getSort());
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        role.setRemark(request.getRemark());
        ensureStatus(role.getStatus());
        sysRoleMapper.insert(role);
        return toVO(role);
    }

    @Transactional
    public RoleVO update(Long id, RoleUpdateRequest request) {
        SysRole role = requireRole(id);
        role.setRoleName(request.getRoleName());
        role.setSort(request.getSort() == null ? role.getSort() : request.getSort());
        if (request.getStatus() != null) {
            ensureStatus(request.getStatus());
            role.setStatus(request.getStatus());
        }
        role.setRemark(request.getRemark());
        sysRoleMapper.updateById(role);
        return toVO(requireRole(id));
    }

    @Transactional
    public void delete(Long id) {
        requireRole(id);
        sysRoleMenuMapper.physicalDeleteByRoleId(id);
        sysUserRoleMapper.physicalDeleteByRoleId(id);
        sysRoleMapper.deleteById(id);
    }

    @Transactional
    public void assignMenus(Long id, AssignRoleMenusRequest request) {
        requireRole(id);
        List<Long> menuIds = request.getMenuIds().stream().distinct().toList();
        ensureMenusExist(menuIds);
        sysRoleMenuMapper.physicalDeleteByRoleId(id);
        menuIds.forEach(menuId -> {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(id);
            relation.setMenuId(menuId);
            sysRoleMenuMapper.insert(relation);
        });
    }

    private SysRole requireRole(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("role not found");
        }
        return role;
    }

    private void ensureRoleCodeAvailable(String roleCode) {
        Long count = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode));
        if (count > 0) {
            throw new BusinessException("role code already exists");
        }
    }

    private void ensureMenusExist(List<Long> menuIds) {
        if (CollectionUtils.isEmpty(menuIds)) {
            return;
        }
        List<SysMenu> menus = sysMenuMapper.selectBatchIds(menuIds);
        if (menus.size() != menuIds.size()) {
            throw new BusinessException("menu not found");
        }
    }

    private void ensureStatus(Integer status) {
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException("status must be 0 or 1");
        }
    }

    private String normalizeRoleCode(String roleCode) {
        return roleCode.trim().toUpperCase();
    }

    private RoleVO toVO(SysRole role) {
        List<Long> menuIds = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, role.getId()))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .toList();
        return RoleVO.builder()
                .id(role.getId())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .sort(role.getSort())
                .status(role.getStatus())
                .remark(role.getRemark())
                .menuIds(menuIds)
                .build();
    }
}
