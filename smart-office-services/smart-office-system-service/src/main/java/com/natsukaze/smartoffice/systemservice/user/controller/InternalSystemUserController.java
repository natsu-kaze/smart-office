package com.natsukaze.smartoffice.systemservice.user.controller;

import com.natsukaze.smartoffice.api.system.dto.CurrentUserDTO;
import com.natsukaze.smartoffice.api.system.dto.SystemAuthUserDTO;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.systemservice.user.service.SystemUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/system/users")
@RequiredArgsConstructor
public class InternalSystemUserController {

    private final SystemUserService systemUserService;

    @GetMapping("/{userId}")
    public Result<CurrentUserDTO> getById(@PathVariable Long userId) {
        return Result.success(systemUserService.getCurrentUser(userId));
    }

    @GetMapping("/username/{username}")
    public Result<SystemAuthUserDTO> getByUsername(@PathVariable String username) {
        return Result.success(systemUserService.getAuthUserByUsername(username));
    }

    @GetMapping("/roles/{roleCode}/first-user")
    public Result<CurrentUserDTO> getFirstUserByRole(@PathVariable String roleCode) {
        return Result.success(systemUserService.getFirstUserByRole(roleCode));
    }

    @PutMapping("/{userId}/last-login")
    public Result<Void> updateLastLoginTime(@PathVariable Long userId) {
        systemUserService.updateLastLoginTime(userId);
        return Result.success();
    }
}
