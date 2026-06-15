package com.natsukaze.smartoffice.attendance.dto;

import com.natsukaze.smartoffice.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceRecordQuery extends PageQuery {

    private Long userId;

    private Long departmentId;

    private LocalDate startDate;

    private LocalDate endDate;
}
