package com.natsukaze.smartoffice.systemservice.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import com.natsukaze.smartoffice.systemservice.user.dto.RoleMenuAssignRequest;
import com.natsukaze.smartoffice.systemservice.user.dto.RolePageQuery;
import com.natsukaze.smartoffice.systemservice.user.dto.RoleSaveRequest;
import com.natsukaze.smartoffice.systemservice.user.dto.UserRoleAssignRequest;
import com.natsukaze.smartoffice.systemservice.user.entity.SysMenu;
import com.natsukaze.smartoffice.systemservice.user.entity.SysRole;
import com.natsukaze.smartoffice.systemservice.user.entity.SysRoleMenu;
import com.natsukaze.smartoffice.systemservice.user.entity.SysUserRole;
import com.natsukaze.smartoffice.systemservice.user.mapper.SysMenuMapper;
import com.natsukaze.smartoffice.systemservice.user.mapper.SysRoleMapper;
import com.natsukaze.smartoffice.systemservice.user.mapper.SysRoleMenuMapper;
import com.natsukaze.smartoffice.systemservice.user.mapper.SysUserMapper;
import com.natsukaze.smartoffice.systemservice.user.mapper.SysUserRoleMapper;
import com.natsukaze.smartoffice.systemservice.user.vo.MenuVO;
import com.natsukaze.smartoffice.systemservice.user.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SystemPermissionService {

    private static final String DEFAULT_ROLE = "EMPLOYEE";

    private static final Map<String, List<String>> DEFAULT_ROLE_PERMISSIONS = Map.of(
            "ADMIN", List.of(
                    "dashboard:view",
                    "sys:user:list",
                    "sys:user:role",
                    "sys:role:list",
                    "sys:role:save",
                    "sys:role:menu",
                    "org:manage",
                    "approval:list",
                    "message:list",
                    "message:announcement:send",
                    "file:list",
                    "file:delete",
                    "policy:list",
                    "policy:manage",
                    "attendance:list",
                    "attendance:department:list"
            ),
            "MANAGER", List.of(
                    "dashboard:view",
                    "org:manage",
                    "approval:list",
                    "message:list",
                    "message:announcement:send",
                    "file:list",
                    "policy:list",
                    "policy:manage",
                    "attendance:list",
                    "attendance:department:list"
            ),
            "EMPLOYEE", List.of(
                    "dashboard:view",
                    "approval:list",
                    "message:list",
                    "file:list",
                    "policy:list",
                    "attendance:list"
            ),
            "FINANCE", List.of(
                    "dashboard:view",
                    "approval:list",
                    "message:list",
                    "file:list",
                    "policy:list",
                    "attendance:list"
            )
    );

    private final SysRoleMapper roleMapper;

    private final SysMenuMapper menuMapper;

    private final SysRoleMenuMapper roleMenuMapper;

    private final SysUserRoleMapper userRoleMapper;

    private final SysUserMapper userMapper;

    public PageResult<RoleVO> pageRoles(RolePageQuery query) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(SysRole::getRoleCode, query.getKeyword())
                        .or()
                        .like(SysRole::getRoleName, query.getKeyword()))
                .orderByAsc(SysRole::getSort)
                .orderByDesc(SysRole::getCreateTime);
        Page<SysRole> page = roleMapper.selectPage(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.from(page.convert(this::toRoleVO));
    }

    public List<RoleVO> listEnabledRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getStatus, 1)
                        .orderByAsc(SysRole::getSort))
                .stream()
                .map(this::toRoleVO)
                .toList();
    }

    @Transactional
    public RoleVO createRole(RoleSaveRequest request) {
        ensureRoleCodeUnique(null, request.getRoleCode());
        SysRole role = new SysRole();
        fillRole(role, request);
        roleMapper.insert(role);
        return toRoleVO(role);
    }

    @Transactional
    public RoleVO updateRole(Long roleId, RoleSaveRequest request) {
        SysRole role = getRequiredRole(roleId);
        ensureRoleCodeUnique(roleId, request.getRoleCode());
        fillRole(role, request);
        roleMapper.updateById(role);
        return toRoleVO(role);
    }

    @Transactional
    public void deleteRole(Long roleId) {
        getRequiredRole(roleId);
        long userCount = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, roleId));
        if (userCount > 0) {
            throw new BusinessException("role is assigned to users");
        }
        roleMenuMapper.physicalDeleteByRoleId(roleId);
        roleMapper.deleteById(roleId);
    }

    public List<MenuVO> treeMenus() {
        List<MenuVO> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                        .orderByAsc(SysMenu::getSort)
                        .orderByAsc(SysMenu::getId))
                .stream()
                .map(this::toMenuVO)
                .toList();
        return buildTree(menus);
    }

    public List<Long> listRoleMenuIds(Long roleId) {
        getRequiredRole(roleId);
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .toList();
    }

    @Transactional
    public void assignRoleMenus(Long roleId, RoleMenuAssignRequest request) {
        getRequiredRole(roleId);
        roleMenuMapper.physicalDeleteByRoleId(roleId);
        if (CollectionUtils.isEmpty(request.getMenuIds())) {
            return;
        }
        request.getMenuIds().stream().distinct().forEach(menuId -> {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        });
    }

    @Transactional
    public void assignUserRoles(Long userId, UserRoleAssignRequest request) {
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
        userRoleMapper.physicalDeleteByUserId(userId);
        if (CollectionUtils.isEmpty(request.getRoleIds())) {
            return;
        }
        request.getRoleIds().stream().distinct().forEach(roleId -> {
            getRequiredRole(roleId);
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        });
    }

    public List<String> listPermissionsByUserId(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .toList();
        if (CollectionUtils.isEmpty(roleIds)) {
            return DEFAULT_ROLE_PERMISSIONS.get(DEFAULT_ROLE);
        }
        Set<String> permissions = new LinkedHashSet<>();
        List<Long> menuIds = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .in(SysRoleMenu::getRoleId, roleIds))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(menuIds)) {
            roleMapper.selectBatchIds(roleIds).stream()
                    .filter(role -> Integer.valueOf(1).equals(role.getStatus()))
                    .map(SysRole::getRoleCode)
                    .filter(StringUtils::hasText)
                    .map(String::toUpperCase)
                    .map(DEFAULT_ROLE_PERMISSIONS::get)
                    .filter(list -> !CollectionUtils.isEmpty(list))
                    .forEach(permissions::addAll);
            return new ArrayList<>(permissions);
        }
        menuMapper.selectBatchIds(menuIds).stream()
                .filter(menu -> Integer.valueOf(1).equals(menu.getStatus()))
                .map(SysMenu::getPermission)
                .filter(StringUtils::hasText)
                .forEach(permissions::add);
        return new ArrayList<>(permissions);
    }

    private void ensureRoleCodeUnique(Long roleId, String roleCode) {
        SysRole exists = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode)
                .last("LIMIT 1"));
        if (exists != null && !exists.getId().equals(roleId)) {
            throw new BusinessException("role code already exists");
        }
    }

    private SysRole getRequiredRole(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "role not found");
        }
        return role;
    }

    private void fillRole(SysRole role, RoleSaveRequest request) {
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setSort(request.getSort() == null ? 0 : request.getSort());
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        role.setRemark(request.getRemark());
    }

    private RoleVO toRoleVO(SysRole role) {
        return RoleVO.builder()
                .id(role.getId())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .sort(role.getSort())
                .status(role.getStatus())
                .remark(role.getRemark())
                .menuIds(listRoleMenuIdsSilently(role.getId()))
                .build();
    }

    private List<Long> listRoleMenuIdsSilently(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .toList();
    }

    private MenuVO toMenuVO(SysMenu menu) {
        return MenuVO.builder()
                .id(menu.getId())
                .parentId(menu.getParentId())
                .menuName(menu.getMenuName())
                .menuType(menu.getMenuType())
                .path(menu.getPath())
                .component(menu.getComponent())
                .permission(menu.getPermission())
                .icon(menu.getIcon())
                .sort(menu.getSort())
                .visible(menu.getVisible())
                .status(menu.getStatus())
                .children(new ArrayList<>())
                .build();
    }

    private List<MenuVO> buildTree(List<MenuVO> menus) {
        Map<Long, MenuVO> menuMap = new LinkedHashMap<>();
        menus.forEach(menu -> menuMap.put(menu.getId(), menu));
        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO menu : menus) {
            MenuVO parent = menuMap.get(menu.getParentId());
            if (parent == null || Long.valueOf(0L).equals(menu.getParentId())) {
                roots.add(menu);
            } else {
                parent.getChildren().add(menu);
            }
        }
        Comparator<MenuVO> comparator = Comparator
                .comparing((MenuVO menu) -> menu.getSort() == null ? 0 : menu.getSort())
                .thenComparing(MenuVO::getId);
        sortTree(roots, comparator);
        return roots;
    }

    private void sortTree(List<MenuVO> menus, Comparator<MenuVO> comparator) {
        menus.sort(comparator);
        menus.forEach(menu -> sortTree(menu.getChildren(), comparator));
    }
}
