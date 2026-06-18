package com.natsukaze.smartoffice.attendanceservice.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceJobResultVO {

    private String jobName;

    private String targetDate;

    private String targetMonth;

    private Integer totalUsers;

    private Integer recordsInserted;

    private Integer recordsUpdated;

    private Integer summariesUpdated;
}
