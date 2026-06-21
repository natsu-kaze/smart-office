package com.natsukaze.smartoffice.api.org.client;

import com.natsukaze.smartoffice.api.org.dto.OrgEmployeeDTO;
import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.core.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrgEmployeeClientFallback implements FallbackFactory<OrgEmployeeClient> {

    private static final Logger log = LoggerFactory.getLogger(OrgEmployeeClientFallback.class);
    private static final String UNAVAILABLE = "org service unavailable";

    @Override
    public OrgEmployeeClient create(Throwable cause) {
        log.error("OrgEmployeeClient fallback triggered", cause);
        return new OrgEmployeeClient() {
            @Override
            public Result<OrgEmployeeDTO> getByUserId(Long userId) {
                return failResult(UNAVAILABLE);
            }

            @Override
            public Result<List<Long>> listUserIdsByDepartmentId(Long departmentId) {
                return failResult(UNAVAILABLE);
            }

            @Override
            public Result<List<Long>> listUserIdsByDepartmentSubtree(Long departmentId) {
                return failResult(UNAVAILABLE);
            }

            @Override
            public Result<List<Long>> listLeaderUserIdsByDepartmentSubtree(Long departmentId) {
                return failResult(UNAVAILABLE);
            }

            @Override
            public Result<List<Long>> listLeaderUserIds() {
                return failResult(UNAVAILABLE);
            }

            @Override
            public Result<List<Long>> listActiveUserIds() {
                return failResult(UNAVAILABLE);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> Result<T> failResult(String message) {
        return (Result<T>) new Result<>(ErrorCode.SYSTEM_ERROR.getCode(), message, null);
    }
}
