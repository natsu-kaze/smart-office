package com.natsukaze.smartoffice.attendance.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AttendanceSummaryVO {

    private Long userId;

    private String realName;

    private String summaryMonth;

    private Integer normalDays;

    private Integer lateCount;

    private Integer earlyLeaveCount;

    private Integer missingCount;

    private BigDecimal leaveDays;

    private BigDecimal overtimeHours;
}
