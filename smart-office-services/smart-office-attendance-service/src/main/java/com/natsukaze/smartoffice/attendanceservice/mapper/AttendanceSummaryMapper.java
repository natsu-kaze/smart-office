package com.natsukaze.smartoffice.attendanceservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.natsukaze.smartoffice.attendanceservice.entity.AttendanceSummary;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttendanceSummaryMapper extends BaseMapper<AttendanceSummary> {
}

