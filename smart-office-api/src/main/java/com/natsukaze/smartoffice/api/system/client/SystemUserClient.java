package com.natsukaze.smartoffice.api.system.client;

import com.natsukaze.smartoffice.api.system.dto.CurrentUserDTO;
import com.natsukaze.smartoffice.api.system.dto.SystemAuthUserDTO;
import com.natsukaze.smartoffice.common.constants.ServiceNames;
import com.natsukaze.smartoffice.common.core.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = ServiceNames.SYSTEM, path = "/internal/system/users",
        fallbackFactory = SystemUserClientFallback.class)
public interface SystemUserClient {

    @GetMapping("/{userId}")
    Result<CurrentUserDTO> getById(@PathVariable("userId") Long userId);

    @GetMapping("/username/{username}")
    Result<SystemAuthUserDTO> getByUsername(@PathVariable("username") String username);

    @GetMapping("/roles/{roleCode}/first-user")
    Result<CurrentUserDTO> getFirstUserByRole(@PathVariable("roleCode") String roleCode);

    @PutMapping("/{userId}/last-login")
    Result<Void> updateLastLoginTime(@PathVariable("userId") Long userId);

    @GetMapping("/{userId}/has-role/{roleCode}")
    Result<Boolean> hasRole(@PathVariable("userId") Long userId,
                            @PathVariable("roleCode") String roleCode);
}
