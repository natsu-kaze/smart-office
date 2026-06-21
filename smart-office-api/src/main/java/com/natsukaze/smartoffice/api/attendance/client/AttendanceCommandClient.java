package com.natsukaze.smartoffice.api.attendance.client;

import com.natsukaze.smartoffice.api.attendance.dto.LeaveAttendanceCommand;
import com.natsukaze.smartoffice.common.constants.ServiceNames;
import com.natsukaze.smartoffice.common.core.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = ServiceNames.ATTENDANCE, path = "/internal/attendance/commands",
        fallbackFactory = AttendanceCommandClientFallback.class)
public interface AttendanceCommandClient {

    @PostMapping("/leave")
    Result<Void> markLeave(@RequestBody LeaveAttendanceCommand command);
}
