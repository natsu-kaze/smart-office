package com.natsukaze.smartoffice.attendanceservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("attendance_summary")
public class AttendanceSummary extends BaseEntity {

    private Long userId;

    private String summaryMonth;

    private Integer normalDays;

    private Integer lateCount;

    private Integer earlyLeaveCount;

    private Integer missingCount;

    private BigDecimal leaveDays;

    private BigDecimal overtimeHours;
}

