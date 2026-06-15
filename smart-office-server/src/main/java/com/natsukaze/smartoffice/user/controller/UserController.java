package com.natsukaze.smartoffice.user.controller;

import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.user.dto.AssignUserRolesRequest;
import com.natsukaze.smartoffice.user.dto.UserCreateRequest;
import com.natsukaze.smartoffice.user.dto.UserPageQuery;
import com.natsukaze.smartoffice.user.dto.UserStatusRequest;
import com.natsukaze.smartoffice.user.dto.UserUpdateRequest;
import com.natsukaze.smartoffice.user.service.UserManagementService;
import com.natsukaze.smartoffice.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userManagementService;

    @GetMapping
    public Result<PageResult<UserVO>> page(@ModelAttribute UserPageQuery query) {
        return Result.success(userManagementService.page(query));
    }

    @PostMapping
    public Result<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userManagementService.create(request));
    }

    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return Result.success(userManagementService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        userManagementService.updateStatus(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userManagementService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignUserRolesRequest request) {
        userManagementService.assignRoles(id, request);
        return Result.success();
    }
}
