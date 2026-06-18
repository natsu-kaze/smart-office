package com.natsukaze.smartoffice.attendanceservice.controller;

import com.natsukaze.smartoffice.attendanceservice.service.AttendanceJobService;
import com.natsukaze.smartoffice.attendanceservice.vo.AttendanceJobResultVO;
import com.natsukaze.smartoffice.common.core.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/internal/attendance/jobs")
@RequiredArgsConstructor
public class InternalAttendanceJobController {

    private final AttendanceJobService attendanceJobService;

    @PostMapping("/daily-settlement")
    public Result<AttendanceJobResultVO> dailySettlement(@RequestParam(required = false) String date) {
        LocalDate targetDate = date == null || date.isBlank() ? LocalDate.now().minusDays(1) : LocalDate.parse(date);
        return Result.success(attendanceJobService.settleDaily(targetDate));
    }

    @PostMapping("/monthly-summary")
    public Result<AttendanceJobResultVO> monthlySummary(@RequestParam(required = false) String month) {
        YearMonth targetMonth = month == null || month.isBlank() ? YearMonth.now().minusMonths(1) : YearMonth.parse(month);
        return Result.success(attendanceJobService.summarizeMonth(targetMonth));
    }
}
