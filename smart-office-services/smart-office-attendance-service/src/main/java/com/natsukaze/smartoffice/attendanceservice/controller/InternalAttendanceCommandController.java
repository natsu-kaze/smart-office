package com.natsukaze.smartoffice.attendanceservice.controller;

import com.natsukaze.smartoffice.api.attendance.dto.LeaveAttendanceCommand;
import com.natsukaze.smartoffice.attendanceservice.service.AttendanceService;
import com.natsukaze.smartoffice.common.core.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/attendance/commands")
@RequiredArgsConstructor
public class InternalAttendanceCommandController {

    private final AttendanceService attendanceService;

    @PostMapping("/leave")
    public Result<Void> markLeave(@RequestBody LeaveAttendanceCommand command) {
        attendanceService.markLeave(command);
        return Result.success();
    }
}
