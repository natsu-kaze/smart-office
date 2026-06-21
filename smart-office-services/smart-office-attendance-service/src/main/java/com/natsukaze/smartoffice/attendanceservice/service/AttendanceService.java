package com.natsukaze.smartoffice.attendanceservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.natsukaze.smartoffice.api.attendance.dto.LeaveAttendanceCommand;
import com.natsukaze.smartoffice.api.org.client.OrgEmployeeClient;
import com.natsukaze.smartoffice.api.org.dto.OrgEmployeeDTO;
import com.natsukaze.smartoffice.api.system.client.SystemUserClient;
import com.natsukaze.smartoffice.api.system.dto.CurrentUserDTO;
import com.natsukaze.smartoffice.api.system.dto.SystemAuthUserDTO;
import com.natsukaze.smartoffice.attendanceservice.dto.AttendanceRecordQuery;
import com.natsukaze.smartoffice.attendanceservice.dto.AttendanceRuleSaveRequest;
import com.natsukaze.smartoffice.attendanceservice.entity.AttendanceRecord;
import com.natsukaze.smartoffice.attendanceservice.entity.AttendanceRule;
import com.natsukaze.smartoffice.attendanceservice.entity.AttendanceSummary;
import com.natsukaze.smartoffice.attendanceservice.mapper.AttendanceRecordMapper;
import com.natsukaze.smartoffice.attendanceservice.mapper.AttendanceRuleMapper;
import com.natsukaze.smartoffice.attendanceservice.mapper.AttendanceSummaryMapper;
import com.natsukaze.smartoffice.attendanceservice.vo.AttendanceRecordVO;
import com.natsukaze.smartoffice.attendanceservice.vo.AttendanceRuleVO;
import com.natsukaze.smartoffice.attendanceservice.vo.AttendanceSummaryVO;
import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.common.enums.AttendanceStatus;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRuleMapper ruleMapper;

    private final AttendanceRecordMapper recordMapper;

    private final AttendanceSummaryMapper summaryMapper;

    private final SystemUserClient systemUserClient;

    private final OrgEmployeeClient orgEmployeeClient;

    @Transactional
    public AttendanceRecordVO checkIn(Long userId) {
        return checkIn(userId, null);
    }

    @Transactional
    public AttendanceRecordVO checkIn(Long userId, String remark) {
        requireUser(userId);
        LocalDate today = LocalDate.now();
        AttendanceRule rule = activeRule();
        AttendanceRecord record = findOrCreateTodayRecord(userId, today);
        ensureNotLeave(record);
        if (record.getCheckInTime() != null) {
            throw new BusinessException("already checked in");
        }
        if (record.getCheckOutTime() != null) {
            throw new BusinessException("今日已下班打卡，无法再上班打卡");
        }
        LocalDateTime now = LocalDateTime.now();
        record.setCheckInTime(now);
        record.setCheckInStatus(now.toLocalTime().isAfter(rule.getWorkStartTime().plusMinutes(rule.getLateMinutes()))
                ? AttendanceStatus.LATE : AttendanceStatus.NORMAL);
        if (remark != null && !remark.isBlank()) {
            record.setRemark(remark);
        }
        saveRecord(record);
        return toRecordVO(record);
    }

    @Transactional
    public AttendanceRecordVO checkOut(Long userId) {
        return checkOut(userId, null);
    }

    @Transactional
    public AttendanceRecordVO checkOut(Long userId, String remark) {
        requireUser(userId);
        LocalDate today = LocalDate.now();
        AttendanceRule rule = activeRule();
        AttendanceRecord record = findOrCreateTodayRecord(userId, today);
        ensureNotLeave(record);
        if (record.getCheckOutTime() != null) {
            throw new BusinessException("already checked out");
        }
        if (record.getCheckInTime() == null) {
            throw new BusinessException("请先完成上班打卡");
        }
        LocalDateTime now = LocalDateTime.now();
        record.setCheckOutTime(now);
        record.setCheckOutStatus(now.toLocalTime().isBefore(rule.getWorkEndTime().minusMinutes(rule.getEarlyLeaveMinutes()))
                ? AttendanceStatus.EARLY_LEAVE : AttendanceStatus.NORMAL);
        if (remark != null && !remark.isBlank()) {
            record.setRemark(remark);
        }
        saveRecord(record);
        return toRecordVO(record);
    }

    public AttendanceRecordVO today(Long userId) {
        requireUser(userId);
        AttendanceRecord record = recordMapper.selectOne(new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getUserId, userId)
                .eq(AttendanceRecord::getAttendanceDate, LocalDate.now())
                .last("LIMIT 1"));
        if (record == null) {
            record = new AttendanceRecord();
            record.setUserId(userId);
            record.setAttendanceDate(LocalDate.now());
        }
        return toRecordVO(record);
    }

    public PageResult<AttendanceRecordVO> personalRecords(Long userId, AttendanceRecordQuery query) {
        query.setUserId(userId);
        return pageRecords(query);
    }

    public PageResult<AttendanceRecordVO> departmentRecords(Long operatorUserId, String username, AttendanceRecordQuery query) {
        ensureAttendanceManager(operatorUserId, username);
        if (query.getDepartmentId() == null) {
            OrgEmployeeDTO employee = safeEmployee(operatorUserId);
            if (employee == null || employee.departmentId() == null) {
                throw new BusinessException("departmentId is required");
            }
            query.setDepartmentId(employee.departmentId());
        }
        return pageRecords(query);
    }

    public PageResult<AttendanceRecordVO> pageRecords(AttendanceRecordQuery query) {
        List<Long> userIds = null;
        if (query.getDepartmentId() != null) {
            userIds = userIdsByDepartment(query.getDepartmentId());
            if (CollectionUtils.isEmpty(userIds)) {
                return PageResult.empty(query.getCurrent(), query.getSize());
            }
        }
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<AttendanceRecord>()
                .eq(query.getUserId() != null, AttendanceRecord::getUserId, query.getUserId())
                .in(userIds != null, AttendanceRecord::getUserId, userIds)
                .ge(query.getStartDate() != null, AttendanceRecord::getAttendanceDate, query.getStartDate())
                .le(query.getEndDate() != null, AttendanceRecord::getAttendanceDate, query.getEndDate())
                .orderByDesc(AttendanceRecord::getAttendanceDate);
        Page<AttendanceRecord> page = recordMapper.selectPage(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.from(page.convert(this::toRecordVO));
    }

    public AttendanceSummaryVO monthlySummary(Long userId, String month) {
        requireUser(userId);
        String summaryMonth = month == null || month.isBlank() ? YearMonth.now().toString() : month;
        AttendanceSummary summary = summaryMapper.selectOne(new LambdaQueryWrapper<AttendanceSummary>()
                .eq(AttendanceSummary::getUserId, userId)
                .eq(AttendanceSummary::getSummaryMonth, summaryMonth)
                .last("LIMIT 1"));
        if (summary == null) {
            return buildSummaryFromRecords(userId, summaryMonth);
        }
        return toSummaryVO(summary);
    }

    @Transactional
    public void markLeave(LeaveAttendanceCommand command) {
        if (command == null || command.userId() == null || command.startDate() == null || command.endDate() == null) {
            throw new BusinessException("leave attendance command is invalid");
        }
        if (command.endDate().isBefore(command.startDate())) {
            throw new BusinessException("leave end date cannot be before start date");
        }
        long days = ChronoUnit.DAYS.between(command.startDate(), command.endDate()) + 1;
        if (days > 31) {
            throw new BusinessException("leave range cannot exceed 31 days");
        }
        for (int i = 0; i < days; i++) {
            LocalDate date = command.startDate().plusDays(i);
            AttendanceRecord record = recordMapper.selectOne(new LambdaQueryWrapper<AttendanceRecord>()
                    .eq(AttendanceRecord::getUserId, command.userId())
                    .eq(AttendanceRecord::getAttendanceDate, date)
                    .last("LIMIT 1"));
            if (record == null) {
                record = new AttendanceRecord();
                record.setUserId(command.userId());
                record.setAttendanceDate(date);
            }
            record.setCheckInStatus(AttendanceStatus.LEAVE);
            record.setCheckOutStatus(AttendanceStatus.LEAVE);
            record.setRemark("Leave approved by approval form #" + command.approvalId());
            saveRecord(record);
        }
        YearMonth startMonth = YearMonth.from(command.startDate());
        YearMonth endMonth = YearMonth.from(command.endDate());
        YearMonth cursor = startMonth;
        while (!cursor.isAfter(endMonth)) {
            upsertMonthlySummary(command.userId(), cursor);
            cursor = cursor.plusMonths(1);
        }
    }

    public AttendanceRuleVO getRule() {
        return toRuleVO(activeRule());
    }

    @Transactional
    public AttendanceRuleVO saveRule(AttendanceRuleSaveRequest request) {
        AttendanceRule rule = activeRule();
        rule.setRuleName(request.getRuleName());
        rule.setWorkStartTime(request.getWorkStartTime());
        rule.setWorkEndTime(request.getWorkEndTime());
        rule.setLateMinutes(request.getLateMinutes() == null ? 0 : request.getLateMinutes());
        rule.setEarlyLeaveMinutes(request.getEarlyLeaveMinutes() == null ? 0 : request.getEarlyLeaveMinutes());
        rule.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        if (rule.getId() == null) {
            ruleMapper.insert(rule);
        } else {
            ruleMapper.updateById(rule);
        }
        return toRuleVO(rule);
    }

    private AttendanceRecord findOrCreateTodayRecord(Long userId, LocalDate today) {
        AttendanceRecord record = recordMapper.selectOne(new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getUserId, userId)
                .eq(AttendanceRecord::getAttendanceDate, today)
                .last("LIMIT 1"));
        if (record != null) {
            return record;
        }
        record = new AttendanceRecord();
        record.setUserId(userId);
        record.setAttendanceDate(today);
        return record;
    }

    private void ensureNotLeave(AttendanceRecord record) {
        if (record.getCheckInStatus() == AttendanceStatus.LEAVE
                || record.getCheckOutStatus() == AttendanceStatus.LEAVE) {
            throw new BusinessException("今天已审批为请假，无需打卡");
        }
    }

    private void saveRecord(AttendanceRecord record) {
        if (record.getId() == null) {
            recordMapper.insert(record);
        } else {
            recordMapper.updateById(record);
        }
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

    private CurrentUserDTO requireUser(Long id) {
        CurrentUserDTO user = safeUser(id);
        if (user == null) {
            throw new BusinessException("user not found");
        }
        return user;
    }

    private void ensureAttendanceManager(Long operatorUserId, String username) {
        requireUser(operatorUserId);
        Result<SystemAuthUserDTO> result = systemUserClient.getByUsername(username);
        if (!success(result) || result.data() == null || result.data().roles() == null) {
            throw new BusinessException("permission denied");
        }
        boolean allowed = result.data().roles().stream()
                .anyMatch(role -> "ADMIN".equals(role) || "MANAGER".equals(role));
        if (!allowed) {
            throw new BusinessException("permission denied");
        }
        if (result.data().roles().stream().noneMatch("ADMIN"::equals)
                && safeEmployee(operatorUserId) == null) {
            throw new BusinessException("department not found");
        }
    }

    private AttendanceSummaryVO buildSummaryFromRecords(Long userId, String month) {
        YearMonth yearMonth = YearMonth.parse(month);
        List<AttendanceRecord> records = recordMapper.selectList(new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getUserId, userId)
                .ge(AttendanceRecord::getAttendanceDate, yearMonth.atDay(1))
                .le(AttendanceRecord::getAttendanceDate, yearMonth.atEndOfMonth()));
        int lateCount = (int) records.stream().filter(record -> record.getCheckInStatus() == AttendanceStatus.LATE).count();
        int earlyLeaveCount = (int) records.stream().filter(record -> record.getCheckOutStatus() == AttendanceStatus.EARLY_LEAVE).count();
        int normalDays = (int) records.stream()
                .filter(record -> record.getCheckInStatus() == AttendanceStatus.NORMAL
                        && record.getCheckOutStatus() == AttendanceStatus.NORMAL)
                .count();
        int missingCount = records.stream().mapToInt(this::missingPunchCount).sum();
        long leaveDays = records.stream()
                .filter(record -> record.getCheckInStatus() == AttendanceStatus.LEAVE
                        || record.getCheckOutStatus() == AttendanceStatus.LEAVE)
                .count();
        CurrentUserDTO user = safeUser(userId);
        return AttendanceSummaryVO.builder()
                .userId(userId)
                .realName(realName(user))
                .summaryMonth(month)
                .normalDays(normalDays)
                .lateCount(lateCount)
                .earlyLeaveCount(earlyLeaveCount)
                .missingCount(missingCount)
                .leaveDays(java.math.BigDecimal.valueOf(leaveDays))
                .overtimeHours(java.math.BigDecimal.ZERO)
                .build();
    }

    private void upsertMonthlySummary(Long userId, YearMonth month) {
        AttendanceSummaryVO snapshot = buildSummaryFromRecords(userId, month.toString());
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
        summary.setNormalDays(snapshot.getNormalDays());
        summary.setLateCount(snapshot.getLateCount());
        summary.setEarlyLeaveCount(snapshot.getEarlyLeaveCount());
        summary.setMissingCount(snapshot.getMissingCount());
        summary.setLeaveDays(snapshot.getLeaveDays() == null ? BigDecimal.ZERO : snapshot.getLeaveDays());
        summary.setOvertimeHours(snapshot.getOvertimeHours() == null ? BigDecimal.ZERO : snapshot.getOvertimeHours());
        if (creating) {
            summaryMapper.insert(summary);
        } else {
            summaryMapper.updateById(summary);
        }
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

    private AttendanceRecordVO toRecordVO(AttendanceRecord record) {
        CurrentUserDTO user = safeUser(record.getUserId());
        OrgEmployeeDTO employee = safeEmployee(record.getUserId());
        return AttendanceRecordVO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .realName(realName(user))
                .departmentId(employee == null ? null : employee.departmentId())
                .departmentName(employee == null ? null : employee.departmentName())
                .attendanceDate(record.getAttendanceDate())
                .checkInTime(record.getCheckInTime())
                .checkOutTime(record.getCheckOutTime())
                .checkInStatus(record.getCheckInStatus() == null ? null : record.getCheckInStatus().getCode())
                .checkOutStatus(record.getCheckOutStatus() == null ? null : record.getCheckOutStatus().getCode())
                .remark(record.getRemark())
                .build();
    }

    private AttendanceRuleVO toRuleVO(AttendanceRule rule) {
        return AttendanceRuleVO.builder()
                .id(rule.getId())
                .ruleName(rule.getRuleName())
                .workStartTime(rule.getWorkStartTime())
                .workEndTime(rule.getWorkEndTime())
                .lateMinutes(rule.getLateMinutes())
                .earlyLeaveMinutes(rule.getEarlyLeaveMinutes())
                .status(rule.getStatus())
                .build();
    }

    private AttendanceSummaryVO toSummaryVO(AttendanceSummary summary) {
        CurrentUserDTO user = safeUser(summary.getUserId());
        return AttendanceSummaryVO.builder()
                .userId(summary.getUserId())
                .realName(realName(user))
                .summaryMonth(summary.getSummaryMonth())
                .normalDays(summary.getNormalDays())
                .lateCount(summary.getLateCount())
                .earlyLeaveCount(summary.getEarlyLeaveCount())
                .missingCount(summary.getMissingCount())
                .leaveDays(summary.getLeaveDays())
                .overtimeHours(summary.getOvertimeHours())
                .build();
    }

    private CurrentUserDTO safeUser(Long userId) {
        Result<CurrentUserDTO> result = systemUserClient.getById(userId);
        return success(result) ? result.data() : null;
    }

    private OrgEmployeeDTO safeEmployee(Long userId) {
        Result<OrgEmployeeDTO> result = orgEmployeeClient.getByUserId(userId);
        return success(result) ? result.data() : null;
    }

    private List<Long> userIdsByDepartment(Long departmentId) {
        Result<List<Long>> result = orgEmployeeClient.listUserIdsByDepartmentId(departmentId);
        if (!success(result)) {
            throw new BusinessException("department not found");
        }
        return result.data();
    }

    private boolean success(Result<?> result) {
        return result != null && result.code() == ErrorCode.SUCCESS.getCode();
    }

    private String realName(CurrentUserDTO user) {
        return user == null ? null : user.realName();
    }
}

