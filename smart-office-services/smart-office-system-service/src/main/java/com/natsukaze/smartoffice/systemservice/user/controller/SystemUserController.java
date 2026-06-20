package com.natsukaze.smartoffice.systemservice.user.controller;

import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.systemservice.user.dto.PasswordChangeRequest;
import com.natsukaze.smartoffice.systemservice.user.dto.ProfileUpdateRequest;
import com.natsukaze.smartoffice.systemservice.user.dto.UserPageQuery;
import com.natsukaze.smartoffice.systemservice.user.dto.UserRoleAssignRequest;
import com.natsukaze.smartoffice.systemservice.user.service.SystemPermissionService;
import com.natsukaze.smartoffice.systemservice.user.service.SystemUserService;
import com.natsukaze.smartoffice.systemservice.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class SystemUserController {

    private final SystemUserService systemUserService;

    private final SystemPermissionService permissionService;

    @GetMapping
    public Result<PageResult<UserVO>> page(@ModelAttribute UserPageQuery query) {
        return Result.success(systemUserService.page(query));
    }

    @GetMapping("/profile")
    public Result<UserVO> profile(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(systemUserService.profile(userId));
    }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@RequestHeader("X-User-Id") Long userId,
                                        @Valid @RequestBody ProfileUpdateRequest request) {
        return Result.success(systemUserService.updateProfile(userId, request));
    }

    @PutMapping("/profile/password")
    public Result<Void> changePassword(@RequestHeader("X-User-Id") Long userId,
                                       @Valid @RequestBody PasswordChangeRequest request) {
        systemUserService.changePassword(userId, request);
        return Result.success();
    }

    @PutMapping("/{userId}/roles")
    public Result<Void> assignRoles(@PathVariable Long userId, @RequestBody UserRoleAssignRequest request) {
        permissionService.assignUserRoles(userId, request);
        return Result.success();
    }
}
