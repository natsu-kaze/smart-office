package com.natsukaze.smartoffice.attendanceservice.service;

import com.natsukaze.smartoffice.api.org.client.OrgEmployeeClient;
import com.natsukaze.smartoffice.attendanceservice.entity.AttendanceRecord;
import com.natsukaze.smartoffice.attendanceservice.entity.AttendanceSummary;
import com.natsukaze.smartoffice.attendanceservice.mapper.AttendanceRecordMapper;
import com.natsukaze.smartoffice.attendanceservice.mapper.AttendanceRuleMapper;
import com.natsukaze.smartoffice.attendanceservice.mapper.AttendanceSummaryMapper;
import com.natsukaze.smartoffice.attendanceservice.vo.AttendanceJobResultVO;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.common.enums.AttendanceStatus;
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
            insertedRecord.set(invocation.getArgument(0));
            return 1;
        });
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
        when(recordMapper.selectList(any())).thenReturn(List.of(existing));
        when(summaryMapper.selectOne(any())).thenReturn(null);

        AttendanceJobResultVO result = attendanceJobService.settleDaily(date);

        assertThat(result.getRecordsUpdated()).isEqualTo(1);
        assertThat(existing.getCheckInStatus()).isEqualTo(AttendanceStatus.NORMAL);
        assertThat(existing.getCheckOutStatus()).isEqualTo(AttendanceStatus.MISSING);
        verify(recordMapper).updateById(existing);
    }
}
