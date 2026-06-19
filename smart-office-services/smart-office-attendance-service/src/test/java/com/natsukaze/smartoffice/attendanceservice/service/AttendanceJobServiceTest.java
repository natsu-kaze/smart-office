package com.natsukaze.smartoffice.attendanceservice.service;

import com.natsukaze.smartoffice.api.message.client.MessageCommandClient;
import com.natsukaze.smartoffice.api.message.dto.NoticeCreateCommand;
import com.natsukaze.smartoffice.api.org.client.OrgEmployeeClient;
import com.natsukaze.smartoffice.attendanceservice.entity.AttendanceRecord;
import com.natsukaze.smartoffice.attendanceservice.entity.AttendanceSummary;
import com.natsukaze.smartoffice.attendanceservice.mapper.AttendanceRecordMapper;
import com.natsukaze.smartoffice.attendanceservice.mapper.AttendanceRuleMapper;
import com.natsukaze.smartoffice.attendanceservice.mapper.AttendanceSummaryMapper;
import com.natsukaze.smartoffice.attendanceservice.vo.AttendanceJobResultVO;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.common.enums.AttendanceStatus;
import com.natsukaze.smartoffice.common.enums.BusinessType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceJobServiceTest {

    @Mock
    private AttendanceRecordMapper recordMapper;

    @Mock
    private AttendanceRuleMapper ruleMapper;

    @Mock
    private AttendanceSummaryMapper summaryMapper;

    @Mock
    private OrgEmployeeClient orgEmployeeClient;

    @Mock
    private MessageCommandClient messageCommandClient;

    @InjectMocks
    private AttendanceJobService attendanceJobService;

    @Test
    void settleDailyCreatesMissingRecordAndSummary() {
        LocalDate date = LocalDate.of(2026, 6, 18);
        AtomicReference<AttendanceRecord> insertedRecord = new AtomicReference<>();
        when(orgEmployeeClient.listActiveUserIds()).thenReturn(Result.success(List.of(3L)));
        when(ruleMapper.selectOne(any())).thenReturn(null);
        when(recordMapper.selectOne(any())).thenReturn(null);
        when(recordMapper.insert(any(AttendanceRecord.class))).thenAnswer(invocation -> {
            AttendanceRecord record = invocation.getArgument(0);
            record.setId(30L);
            insertedRecord.set(record);
            return 1;
        });
        when(messageCommandClient.createNotice(any(NoticeCreateCommand.class))).thenReturn(Result.success());
        when(recordMapper.selectList(any())).thenAnswer(invocation -> List.of(insertedRecord.get()));
        when(summaryMapper.selectOne(any())).thenReturn(null);

        AttendanceJobResultVO result = attendanceJobService.settleDaily(date);

        assertThat(result.getTotalUsers()).isEqualTo(1);
        assertThat(result.getRecordsInserted()).isEqualTo(1);
        assertThat(result.getSummariesUpdated()).isEqualTo(1);
        assertThat(insertedRecord.get().getCheckInStatus()).isEqualTo(AttendanceStatus.MISSING);
        assertThat(insertedRecord.get().getCheckOutStatus()).isEqualTo(AttendanceStatus.MISSING);

        ArgumentCaptor<AttendanceSummary> summaryCaptor = ArgumentCaptor.forClass(AttendanceSummary.class);
        verify(summaryMapper).insert(summaryCaptor.capture());
        assertThat(summaryCaptor.getValue().getMissingCount()).isEqualTo(2);
        assertThat(summaryCaptor.getValue().getSummaryMonth()).isEqualTo("2026-06");

        ArgumentCaptor<NoticeCreateCommand> noticeCaptor = ArgumentCaptor.forClass(NoticeCreateCommand.class);
        verify(messageCommandClient).createNotice(noticeCaptor.capture());
        assertThat(noticeCaptor.getValue().userId()).isEqualTo(3L);
        assertThat(noticeCaptor.getValue().businessType()).isEqualTo(BusinessType.ATTENDANCE.getCode());
        assertThat(noticeCaptor.getValue().businessId()).isEqualTo(30L);
        assertThat(noticeCaptor.getValue().title()).isEqualTo("Attendance abnormal");
    }

    @Test
    void settleDailyCompletesMissingCheckoutForExistingRecord() {
        LocalDate date = LocalDate.of(2026, 6, 18);
        AttendanceRecord existing = new AttendanceRecord();
        existing.setId(10L);
        existing.setUserId(3L);
        existing.setAttendanceDate(date);
        existing.setCheckInTime(LocalDateTime.of(2026, 6, 18, 8, 55));
        when(orgEmployeeClient.listActiveUserIds()).thenReturn(Result.success(List.of(3L)));
        when(ruleMapper.selectOne(any())).thenReturn(null);
        when(recordMapper.selectOne(any())).thenReturn(existing);
        when(messageCommandClient.createNotice(any(NoticeCreateCommand.class))).thenReturn(Result.success());
        when(recordMapper.selectList(any())).thenReturn(List.of(existing));
        when(summaryMapper.selectOne(any())).thenReturn(null);

        AttendanceJobResultVO result = attendanceJobService.settleDaily(date);

        assertThat(result.getRecordsUpdated()).isEqualTo(1);
        assertThat(existing.getCheckInStatus()).isEqualTo(AttendanceStatus.NORMAL);
        assertThat(existing.getCheckOutStatus()).isEqualTo(AttendanceStatus.MISSING);
        verify(recordMapper).updateById(existing);
    }
}
