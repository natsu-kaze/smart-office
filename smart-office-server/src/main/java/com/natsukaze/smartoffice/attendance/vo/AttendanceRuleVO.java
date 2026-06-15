package com.natsukaze.smartoffice.attendance.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class AttendanceRuleVO {

    private Long id;

    private String ruleName;

    private LocalTime workStartTime;

    private LocalTime workEndTime;

    private Integer lateMinutes;

    private Integer earlyLeaveMinutes;

    private Integer status;
}
