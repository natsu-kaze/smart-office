package com.natsukaze.smartoffice.attendance.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AttendanceRecordVO {

    private Long id;

    private Long userId;

    private String realName;

    private Long departmentId;

    private String departmentName;

    private LocalDate attendanceDate;

    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    private String checkInStatus;

    private String checkOutStatus;

    private String remark;
}
