package com.natsukaze.smartoffice.user.controller;

import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.user.dto.AssignRoleMenusRequest;
import com.natsukaze.smartoffice.user.dto.RoleCreateRequest;
import com.natsukaze.smartoffice.user.dto.RolePageQuery;
import com.natsukaze.smartoffice.user.dto.RoleUpdateRequest;
import com.natsukaze.smartoffice.user.service.RoleManagementService;
import com.natsukaze.smartoffice.user.vo.RoleVO;
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

@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleManagementService roleManagementService;

    @GetMapping
    public Result<PageResult<RoleVO>> page(@ModelAttribute RolePageQuery query) {
        return Result.success(roleManagementService.page(query));
    }

    @PostMapping
    public Result<RoleVO> create(@Valid @RequestBody RoleCreateRequest request) {
        return Result.success(roleManagementService.create(request));
    }

    @PutMapping("/{id}")
    public Result<RoleVO> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return Result.success(roleManagementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleManagementService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody AssignRoleMenusRequest request) {
        roleManagementService.assignMenus(id, request);
        return Result.success();
    }
}
