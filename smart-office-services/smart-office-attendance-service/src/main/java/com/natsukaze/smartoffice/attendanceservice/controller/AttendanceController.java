package com.natsukaze.smartoffice.attendanceservice.controller;

import com.natsukaze.smartoffice.attendanceservice.dto.AttendanceRecordQuery;
import com.natsukaze.smartoffice.attendanceservice.dto.AttendanceRuleSaveRequest;
import com.natsukaze.smartoffice.attendanceservice.service.AttendanceService;
import com.natsukaze.smartoffice.attendanceservice.vo.AttendanceRecordVO;
import com.natsukaze.smartoffice.attendanceservice.vo.AttendanceRuleVO;
import com.natsukaze.smartoffice.attendanceservice.vo.AttendanceSummaryVO;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    public Result<AttendanceRecordVO> checkIn(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(attendanceService.checkIn(userId));
    }

    @PostMapping("/check-out")
    public Result<AttendanceRecordVO> checkOut(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(attendanceService.checkOut(userId));
    }

    @GetMapping("/today")
    public Result<AttendanceRecordVO> today(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(attendanceService.today(userId));
    }

    @GetMapping("/my-records")
    public Result<PageResult<AttendanceRecordVO>> myRecords(@RequestHeader("X-User-Id") Long userId,
                                                            @ModelAttribute AttendanceRecordQuery query) {
        return Result.success(attendanceService.personalRecords(userId, query));
    }

    @GetMapping("/department-records")
    public Result<PageResult<AttendanceRecordVO>> departmentRecords(@ModelAttribute AttendanceRecordQuery query) {
        return Result.success(attendanceService.departmentRecords(query));
    }

    @GetMapping("/summary/monthly")
    public Result<AttendanceSummaryVO> monthlySummary(@RequestHeader("X-User-Id") Long userId,
                                                      @RequestParam(required = false) String month) {
        return Result.success(attendanceService.monthlySummary(userId, month));
    }

    @GetMapping("/rule")
    public Result<AttendanceRuleVO> getRule() {
        return Result.success(attendanceService.getRule());
    }

    @PutMapping("/rule")
    public Result<AttendanceRuleVO> saveRule(@Valid @RequestBody AttendanceRuleSaveRequest request) {
        return Result.success(attendanceService.saveRule(request));
    }
}

