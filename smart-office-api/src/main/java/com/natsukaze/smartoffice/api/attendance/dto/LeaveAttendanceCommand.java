package com.natsukaze.smartoffice.api.attendance.dto;

import java.time.LocalDate;

public record LeaveAttendanceCommand(
        Long userId,
        LocalDate startDate,
        LocalDate endDate,
        Long approvalId
) {
}
