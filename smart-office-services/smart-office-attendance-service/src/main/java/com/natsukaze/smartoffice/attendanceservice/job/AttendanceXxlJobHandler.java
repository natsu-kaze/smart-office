package com.natsukaze.smartoffice.attendanceservice.job;

import com.natsukaze.smartoffice.attendanceservice.service.AttendanceJobService;
import com.natsukaze.smartoffice.attendanceservice.vo.AttendanceJobResultVO;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceXxlJobHandler {

    private final AttendanceJobService attendanceJobService;

    @XxlJob("attendanceDailySettlementJob")
    public void dailySettlement() {
        String param = XxlJobHelper.getJobParam();
        LocalDate date = StringUtils.hasText(param) ? LocalDate.parse(param) : LocalDate.now().minusDays(1);
        AttendanceJobResultVO result = attendanceJobService.settleDaily(date);
        log.info("Attendance daily settlement finished. result={}", result);
        XxlJobHelper.handleSuccess(result.toString());
    }

    @XxlJob("attendanceMonthlySummaryJob")
    public void monthlySummary() {
        String param = XxlJobHelper.getJobParam();
        YearMonth month = StringUtils.hasText(param) ? YearMonth.parse(param) : YearMonth.now().minusMonths(1);
        AttendanceJobResultVO result = attendanceJobService.summarizeMonth(month);
        log.info("Attendance monthly summary finished. result={}", result);
        XxlJobHelper.handleSuccess(result.toString());
    }
}
