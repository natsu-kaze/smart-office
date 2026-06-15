package com.natsukaze.smartoffice.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class AttendanceRuleSaveRequest {

    @NotBlank
    private String ruleName;

    @NotNull
    private LocalTime workStartTime;

    @NotNull
    private LocalTime workEndTime;

    private Integer lateMinutes;

    private Integer earlyLeaveMinutes;

    private Integer status;
}
