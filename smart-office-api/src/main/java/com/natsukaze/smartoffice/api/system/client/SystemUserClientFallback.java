package com.natsukaze.smartoffice.api.system.client;

import com.natsukaze.smartoffice.api.system.dto.CurrentUserDTO;
import com.natsukaze.smartoffice.api.system.dto.SystemAuthUserDTO;
import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.core.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class SystemUserClientFallback implements FallbackFactory<SystemUserClient> {

    private static final Logger log = LoggerFactory.getLogger(SystemUserClientFallback.class);
    private static final String UNAVAILABLE = "system service unavailable";

    @Override
    public SystemUserClient create(Throwable cause) {
        log.error("SystemUserClient fallback triggered", cause);
        return new SystemUserClient() {
            @Override
            public Result<CurrentUserDTO> getById(Long userId) {
                return failResult(UNAVAILABLE);
            }

            @Override
            public Result<SystemAuthUserDTO> getByUsername(String username) {
                return failResult(UNAVAILABLE);
            }

            @Override
            public Result<CurrentUserDTO> getFirstUserByRole(String roleCode) {
                return failResult(UNAVAILABLE);
            }

            @Override
            public Result<Void> updateLastLoginTime(Long userId) {
                return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), UNAVAILABLE);
            }

            @Override
            public Result<Boolean> hasRole(Long userId, String roleCode) {
                return Result.success(false);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> Result<T> failResult(String message) {
        return (Result<T>) new Result<>(ErrorCode.SYSTEM_ERROR.getCode(), message, null);
    }
}
