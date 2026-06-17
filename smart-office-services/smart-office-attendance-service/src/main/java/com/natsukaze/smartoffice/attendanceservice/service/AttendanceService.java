package com.natsukaze.smartoffice.attendanceservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.natsukaze.smartoffice.api.org.client.OrgEmployeeClient;
import com.natsukaze.smartoffice.api.org.dto.OrgEmployeeDTO;
import com.natsukaze.smartoffice.api.system.client.SystemUserClient;
import com.natsukaze.smartoffice.api.system.dto.CurrentUserDTO;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
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
        requireUser(userId);
        LocalDate today = LocalDate.now();
        AttendanceRule rule = activeRule();
        AttendanceRecord record = findOrCreateTodayRecord(userId, today);
        if (record.getCheckInTime() != null) {
            throw new BusinessException("already checked in");
        }
        LocalDateTime now = LocalDateTime.now();
        record.setCheckInTime(now);
        record.setCheckInStatus(now.toLocalTime().isAfter(rule.getWorkStartTime().plusMinutes(rule.getLateMinutes()))
                ? AttendanceStatus.LATE : AttendanceStatus.NORMAL);
        saveRecord(record);
        return toRecordVO(record);
    }

    @Transactional
    public AttendanceRecordVO checkOut(Long userId) {
        requireUser(userId);
        LocalDate today = LocalDate.now();
        AttendanceRule rule = activeRule();
        AttendanceRecord record = findOrCreateTodayRecord(userId, today);
        if (record.getCheckOutTime() != null) {
            throw new BusinessException("already checked out");
        }
        LocalDateTime now = LocalDateTime.now();
        record.setCheckOutTime(now);
        record.setCheckOutStatus(now.toLocalTime().isBefore(rule.getWorkEndTime().minusMinutes(rule.getEarlyLeaveMinutes()))
                ? AttendanceStatus.EARLY_LEAVE : AttendanceStatus.NORMAL);
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

    public PageResult<AttendanceRecordVO> departmentRecords(AttendanceRecordQuery query) {
        if (query.getDepartmentId() == null) {
            throw new BusinessException("departmentId is required");
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
        CurrentUserDTO user = safeUser(userId);
        return AttendanceSummaryVO.builder()
                .userId(userId)
                .realName(realName(user))
                .summaryMonth(month)
                .normalDays(normalDays)
                .lateCount(lateCount)
                .earlyLeaveCount(earlyLeaveCount)
                .missingCount(0)
                .leaveDays(java.math.BigDecimal.ZERO)
                .overtimeHours(java.math.BigDecimal.ZERO)
                .build();
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

