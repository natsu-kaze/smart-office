package com.natsukaze.smartoffice.api.attendance.client;

import com.natsukaze.smartoffice.api.attendance.dto.LeaveAttendanceCommand;
import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.core.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class AttendanceCommandClientFallback implements FallbackFactory<AttendanceCommandClient> {

    private static final Logger log = LoggerFactory.getLogger(AttendanceCommandClientFallback.class);

    @Override
    public AttendanceCommandClient create(Throwable cause) {
        log.error("AttendanceCommandClient fallback triggered", cause);
        return command -> Result.failure(ErrorCode.SYSTEM_ERROR, "attendance service unavailable");
    }
}
