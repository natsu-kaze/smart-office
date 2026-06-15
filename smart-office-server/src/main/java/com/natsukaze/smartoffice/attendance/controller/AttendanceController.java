package com.natsukaze.smartoffice.attendance.controller;

import com.natsukaze.smartoffice.attendance.dto.AttendanceRecordQuery;
import com.natsukaze.smartoffice.attendance.dto.AttendanceRuleSaveRequest;
import com.natsukaze.smartoffice.attendance.service.AttendanceService;
import com.natsukaze.smartoffice.attendance.vo.AttendanceRecordVO;
import com.natsukaze.smartoffice.attendance.vo.AttendanceRuleVO;
import com.natsukaze.smartoffice.attendance.vo.AttendanceSummaryVO;
import com.natsukaze.smartoffice.auth.security.UserPrincipal;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    public Result<AttendanceRecordVO> checkIn(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(attendanceService.checkIn(principal.getUserId()));
    }

    @PostMapping("/check-out")
    public Result<AttendanceRecordVO> checkOut(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(attendanceService.checkOut(principal.getUserId()));
    }

    @GetMapping("/today")
    public Result<AttendanceRecordVO> today(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(attendanceService.today(principal.getUserId()));
    }

    @GetMapping("/my-records")
    public Result<PageResult<AttendanceRecordVO>> myRecords(@AuthenticationPrincipal UserPrincipal principal,
                                                            @ModelAttribute AttendanceRecordQuery query) {
        return Result.success(attendanceService.personalRecords(principal.getUserId(), query));
    }

    @GetMapping("/department-records")
    public Result<PageResult<AttendanceRecordVO>> departmentRecords(@ModelAttribute AttendanceRecordQuery query) {
        return Result.success(attendanceService.departmentRecords(query));
    }

    @GetMapping("/summary/monthly")
    public Result<AttendanceSummaryVO> monthlySummary(@AuthenticationPrincipal UserPrincipal principal,
                                                      @RequestParam(required = false) String month) {
        return Result.success(attendanceService.monthlySummary(principal.getUserId(), month));
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
