package com.natsukaze.smartoffice.systemservice.user.controller;

import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.systemservice.user.dto.RoleMenuAssignRequest;
import com.natsukaze.smartoffice.systemservice.user.dto.RolePageQuery;
import com.natsukaze.smartoffice.systemservice.user.dto.RoleSaveRequest;
import com.natsukaze.smartoffice.systemservice.user.service.SystemPermissionService;
import com.natsukaze.smartoffice.systemservice.user.vo.MenuVO;
import com.natsukaze.smartoffice.systemservice.user.vo.RoleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemPermissionController {

    private final SystemPermissionService permissionService;

    @GetMapping("/roles")
    public Result<PageResult<RoleVO>> pageRoles(@ModelAttribute RolePageQuery query) {
        return Result.success(permissionService.pageRoles(query));
    }

    @GetMapping("/roles/options")
    public Result<List<RoleVO>> listEnabledRoles() {
        return Result.success(permissionService.listEnabledRoles());
    }

    @PostMapping("/roles")
    public Result<RoleVO> createRole(@Valid @RequestBody RoleSaveRequest request) {
        return Result.success(permissionService.createRole(request));
    }

    @PutMapping("/roles/{roleId}")
    public Result<RoleVO> updateRole(@PathVariable Long roleId, @Valid @RequestBody RoleSaveRequest request) {
        return Result.success(permissionService.updateRole(roleId, request));
    }

    @DeleteMapping("/roles/{roleId}")
    public Result<Void> deleteRole(@PathVariable Long roleId) {
        permissionService.deleteRole(roleId);
        return Result.success();
    }

    @GetMapping("/roles/{roleId}/menus")
    public Result<List<Long>> listRoleMenuIds(@PathVariable Long roleId) {
        return Result.success(permissionService.listRoleMenuIds(roleId));
    }

    @PutMapping("/roles/{roleId}/menus")
    public Result<Void> assignRoleMenus(@PathVariable Long roleId, @RequestBody RoleMenuAssignRequest request) {
        permissionService.assignRoleMenus(roleId, request);
        return Result.success();
    }

    @GetMapping("/menus/tree")
    public Result<List<MenuVO>> treeMenus() {
        return Result.success(permissionService.treeMenus());
    }
}
