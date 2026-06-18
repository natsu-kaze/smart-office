package com.natsukaze.smartoffice.attendanceservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.natsukaze.smartoffice.api.org.client.OrgEmployeeClient;
import com.natsukaze.smartoffice.attendanceservice.entity.AttendanceRecord;
import com.natsukaze.smartoffice.attendanceservice.entity.AttendanceRule;
import com.natsukaze.smartoffice.attendanceservice.entity.AttendanceSummary;
import com.natsukaze.smartoffice.attendanceservice.mapper.AttendanceRecordMapper;
import com.natsukaze.smartoffice.attendanceservice.mapper.AttendanceRuleMapper;
import com.natsukaze.smartoffice.attendanceservice.mapper.AttendanceSummaryMapper;
import com.natsukaze.smartoffice.attendanceservice.vo.AttendanceJobResultVO;
import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.common.enums.AttendanceStatus;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceJobService {

    private final AttendanceRecordMapper recordMapper;

    private final AttendanceRuleMapper ruleMapper;

    private final AttendanceSummaryMapper summaryMapper;

    private final OrgEmployeeClient orgEmployeeClient;

    @Transactional
    public AttendanceJobResultVO settleDaily(LocalDate date) {
        List<Long> userIds = activeUserIds();
        AttendanceRule rule = activeRule();
        int inserted = 0;
        int updated = 0;
        for (Long userId : userIds) {
            AttendanceRecord record = recordMapper.selectOne(new LambdaQueryWrapper<AttendanceRecord>()
                    .eq(AttendanceRecord::getUserId, userId)
                    .eq(AttendanceRecord::getAttendanceDate, date)
                    .last("LIMIT 1"));
            if (record == null) {
                record = new AttendanceRecord();
                record.setUserId(userId);
                record.setAttendanceDate(date);
                record.setCheckInStatus(AttendanceStatus.MISSING);
                record.setCheckOutStatus(AttendanceStatus.MISSING);
                record.setRemark("Daily settlement: missing check-in and check-out");
                recordMapper.insert(record);
                inserted++;
                continue;
            }
            if (completeRecordStatus(record, rule)) {
                recordMapper.updateById(record);
                updated++;
            }
        }
        int summaries = refreshMonthlySummaries(YearMonth.from(date), userIds);
        return AttendanceJobResultVO.builder()
                .jobName("attendanceDailySettlementJob")
                .targetDate(date.toString())
                .targetMonth(YearMonth.from(date).toString())
                .totalUsers(userIds.size())
                .recordsInserted(inserted)
                .recordsUpdated(updated)
                .summariesUpdated(summaries)
                .build();
    }

    @Transactional
    public AttendanceJobResultVO summarizeMonth(YearMonth month) {
        List<Long> userIds = activeUserIds();
        int summaries = refreshMonthlySummaries(month, userIds);
        return AttendanceJobResultVO.builder()
                .jobName("attendanceMonthlySummaryJob")
                .targetMonth(month.toString())
                .totalUsers(userIds.size())
                .recordsInserted(0)
                .recordsUpdated(0)
                .summariesUpdated(summaries)
                .build();
    }

    private int refreshMonthlySummaries(YearMonth month, List<Long> userIds) {
        int updated = 0;
        for (Long userId : userIds) {
            upsertSummary(userId, month);
            updated++;
        }
        return updated;
    }

    private void upsertSummary(Long userId, YearMonth month) {
        List<AttendanceRecord> records = recordMapper.selectList(new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getUserId, userId)
                .ge(AttendanceRecord::getAttendanceDate, month.atDay(1))
                .le(AttendanceRecord::getAttendanceDate, month.atEndOfMonth()));
        AttendanceSummary summary = summaryMapper.selectOne(new LambdaQueryWrapper<AttendanceSummary>()
                .eq(AttendanceSummary::getUserId, userId)
                .eq(AttendanceSummary::getSummaryMonth, month.toString())
                .last("LIMIT 1"));
        boolean creating = summary == null;
        if (creating) {
            summary = new AttendanceSummary();
            summary.setUserId(userId);
            summary.setSummaryMonth(month.toString());
        }
        summary.setNormalDays((int) records.stream().filter(this::isNormalDay).count());
        summary.setLateCount((int) records.stream().filter(record -> record.getCheckInStatus() == AttendanceStatus.LATE).count());
        summary.setEarlyLeaveCount((int) records.stream().filter(record -> record.getCheckOutStatus() == AttendanceStatus.EARLY_LEAVE).count());
        summary.setMissingCount(records.stream().mapToInt(this::missingPunchCount).sum());
        summary.setLeaveDays(BigDecimal.valueOf(records.stream().filter(this::isLeaveDay).count()));
        summary.setOvertimeHours(BigDecimal.ZERO);
        if (creating) {
            summaryMapper.insert(summary);
        } else {
            summaryMapper.updateById(summary);
        }
    }

    private boolean completeRecordStatus(AttendanceRecord record, AttendanceRule rule) {
        boolean changed = false;
        StringBuilder remark = new StringBuilder("Daily settlement:");
        if (record.getCheckInStatus() == null) {
            if (record.getCheckInTime() == null) {
                record.setCheckInStatus(AttendanceStatus.MISSING);
                appendRemark(remark, "missing check-in");
            } else {
                record.setCheckInStatus(record.getCheckInTime().toLocalTime()
                        .isAfter(rule.getWorkStartTime().plusMinutes(rule.getLateMinutes()))
                        ? AttendanceStatus.LATE : AttendanceStatus.NORMAL);
                appendRemark(remark, "check-in status completed");
            }
            changed = true;
        }
        if (record.getCheckOutStatus() == null) {
            if (record.getCheckOutTime() == null) {
                record.setCheckOutStatus(AttendanceStatus.MISSING);
                appendRemark(remark, "missing check-out");
            } else {
                record.setCheckOutStatus(record.getCheckOutTime().toLocalTime()
                        .isBefore(rule.getWorkEndTime().minusMinutes(rule.getEarlyLeaveMinutes()))
                        ? AttendanceStatus.EARLY_LEAVE : AttendanceStatus.NORMAL);
                appendRemark(remark, "check-out status completed");
            }
            changed = true;
        }
        if (changed) {
            record.setRemark(remark.toString());
        }
        return changed;
    }

    private void appendRemark(StringBuilder remark, String text) {
        if (remark.length() > "Daily settlement:".length()) {
            remark.append(',');
        }
        remark.append(' ').append(text);
    }

    private boolean isNormalDay(AttendanceRecord record) {
        return record.getCheckInStatus() == AttendanceStatus.NORMAL
                && record.getCheckOutStatus() == AttendanceStatus.NORMAL;
    }

    private boolean isLeaveDay(AttendanceRecord record) {
        return record.getCheckInStatus() == AttendanceStatus.LEAVE
                || record.getCheckOutStatus() == AttendanceStatus.LEAVE;
    }

    private int missingPunchCount(AttendanceRecord record) {
        int count = 0;
        if (record.getCheckInStatus() == AttendanceStatus.MISSING) {
            count++;
        }
        if (record.getCheckOutStatus() == AttendanceStatus.MISSING) {
            count++;
        }
        return count;
    }

    private List<Long> activeUserIds() {
        Result<List<Long>> result = orgEmployeeClient.listActiveUserIds();
        if (result == null || result.code() != ErrorCode.SUCCESS.getCode() || result.data() == null) {
            throw new BusinessException("active employees not found");
        }
        return result.data();
    }

    private AttendanceRule activeRule() {
        AttendanceRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<AttendanceRule>()
                .eq(AttendanceRule::getStatus, 1)
                .orderByAsc(AttendanceRule::getId)
                .last("LIMIT 1"));
        if (rule != null) {
            return rule;
        }
        AttendanceRule defaultRule = new AttendanceRule();
        defaultRule.setRuleName("Default Rule");
        defaultRule.setWorkStartTime(LocalTime.of(9, 0));
        defaultRule.setWorkEndTime(LocalTime.of(18, 0));
        defaultRule.setLateMinutes(0);
        defaultRule.setEarlyLeaveMinutes(0);
        defaultRule.setStatus(1);
        return defaultRule;
    }
}
