package com.natsukaze.smartoffice.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("attendance_rule")
public class AttendanceRule extends BaseEntity {

    private String ruleName;

    private LocalTime workStartTime;

    private LocalTime workEndTime;

    private Integer lateMinutes;

    private Integer earlyLeaveMinutes;

    private Integer status;
}
