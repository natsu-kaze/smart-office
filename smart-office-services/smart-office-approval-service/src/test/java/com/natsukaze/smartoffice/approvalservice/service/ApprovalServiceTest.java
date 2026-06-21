package com.natsukaze.smartoffice.approvalservice.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.natsukaze.smartoffice.api.attendance.client.AttendanceCommandClient;
import com.natsukaze.smartoffice.api.attendance.dto.LeaveAttendanceCommand;
import com.natsukaze.smartoffice.api.message.client.MessageCommandClient;
import com.natsukaze.smartoffice.api.message.dto.NoticeCreateCommand;
import com.natsukaze.smartoffice.api.message.dto.TodoCreateCommand;
import com.natsukaze.smartoffice.api.org.client.OrgEmployeeClient;
import com.natsukaze.smartoffice.api.org.dto.OrgEmployeeDTO;
import com.natsukaze.smartoffice.api.system.client.SystemUserClient;
import com.natsukaze.smartoffice.api.system.dto.CurrentUserDTO;
import com.natsukaze.smartoffice.approvalservice.entity.ApprovalForm;
import com.natsukaze.smartoffice.approvalservice.entity.ApprovalProcess;
import com.natsukaze.smartoffice.approvalservice.entity.ApprovalRecord;
import com.natsukaze.smartoffice.approvalservice.entity.ApprovalRule;
import com.natsukaze.smartoffice.approvalservice.mapper.ApprovalFormMapper;
import com.natsukaze.smartoffice.approvalservice.mapper.ApprovalProcessMapper;
import com.natsukaze.smartoffice.approvalservice.mapper.ApprovalRecordMapper;
import com.natsukaze.smartoffice.approvalservice.mapper.ApprovalRuleMapper;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.common.enums.ApprovalStatus;
import com.natsukaze.smartoffice.common.enums.ApprovalType;
import com.natsukaze.smartoffice.common.enums.BusinessType;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    private static final long FORM_ID = 100L;
    private static final long APPLICANT_ID = 3L;
    private static final long APPROVER_ID = 2L;
    private static final long FINANCE_ID = 4L;

    @Mock
    private ApprovalFormMapper formMapper;

    @Mock
    private ApprovalRecordMapper recordMapper;

    @Mock
    private ApprovalProcessMapper processMapper;

    @Mock
    private ApprovalRuleMapper ruleMapper;

    @Mock
    private SystemUserClient systemUserClient;

    @Mock
    private OrgEmployeeClient orgEmployeeClient;

    @Mock
    private MessageCommandClient messageCommandClient;

    @Mock
    private AttendanceCommandClient attendanceCommandClient;

    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        approvalService = new ApprovalService(
                formMapper,
                recordMapper,
                processMapper,
                ruleMapper,
                systemUserClient,
                orgEmployeeClient,
                messageCommandClient,
                attendanceCommandClient);
        lenient().when(ruleMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
    }

    @Test
    void submitCreatesApprovalProcessTodoAndNotice() {
        ApprovalForm form = draftForm();
        when(formMapper.selectById(FORM_ID)).thenReturn(form);
        when(formMapper.updateById(form)).thenReturn(1);
        stubApplicantEmployee();
        stubDetailLookups(true);
        when(messageCommandClient.createTodo(any(TodoCreateCommand.class))).thenReturn(Result.success());
        when(messageCommandClient.createNotice(any(NoticeCreateCommand.class))).thenReturn(Result.success());

        approvalService.submit(APPLICANT_ID, FORM_ID);

        assertThat(form.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(form.getCurrentApproverId()).isEqualTo(APPROVER_ID);
        assertThat(form.getSubmittedAt()).isNotNull();

        ArgumentCaptor<ApprovalProcess> processCaptor = ArgumentCaptor.forClass(ApprovalProcess.class);
        verify(processMapper).insert(processCaptor.capture());
        assertThat(processCaptor.getValue().getFormId()).isEqualTo(FORM_ID);
        assertThat(processCaptor.getValue().getApproverUserId()).isEqualTo(APPROVER_ID);
        assertThat(processCaptor.getValue().getStatus()).isEqualTo(ApprovalStatus.PENDING);

        ArgumentCaptor<TodoCreateCommand> todoCaptor = ArgumentCaptor.forClass(TodoCreateCommand.class);
        verify(messageCommandClient).createTodo(todoCaptor.capture());
        assertThat(todoCaptor.getValue().userId()).isEqualTo(APPROVER_ID);
        assertThat(todoCaptor.getValue().businessType()).isEqualTo(BusinessType.APPROVAL.getCode());
        assertThat(todoCaptor.getValue().businessId()).isEqualTo(FORM_ID);

        ArgumentCaptor<NoticeCreateCommand> noticeCaptor = ArgumentCaptor.forClass(NoticeCreateCommand.class);
        verify(messageCommandClient).createNotice(noticeCaptor.capture());
        assertThat(noticeCaptor.getValue().userId()).isEqualTo(APPROVER_ID);
        assertThat(noticeCaptor.getValue().businessId()).isEqualTo(FORM_ID);
    }

    @Test
    void approveCompletesTodoUpdatesProcessAndNotifiesApplicant() {
        ApprovalForm form = pendingForm();
        ApprovalProcess process = pendingProcess();
        when(formMapper.selectById(FORM_ID)).thenReturn(form);
        when(formMapper.updateById(form)).thenReturn(1);
        stubApplicantEmployee();
        stubDetailLookups(false);
        when(processMapper.selectOne(any(Wrapper.class))).thenReturn(process);
        when(messageCommandClient.completeTodo(APPROVER_ID, BusinessType.APPROVAL.getCode(), FORM_ID))
                .thenReturn(Result.success());
        when(messageCommandClient.createNotice(any(NoticeCreateCommand.class))).thenReturn(Result.success());
        when(attendanceCommandClient.markLeave(any(LeaveAttendanceCommand.class))).thenReturn(Result.success());

        approvalService.approve(APPROVER_ID, FORM_ID, nullComment());

        assertThat(form.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(form.getCurrentApproverId()).isNull();
        assertThat(form.getCompletedAt()).isNotNull();

        verify(messageCommandClient).completeTodo(APPROVER_ID, BusinessType.APPROVAL.getCode(), FORM_ID);

        ArgumentCaptor<ApprovalProcess> processCaptor = ArgumentCaptor.forClass(ApprovalProcess.class);
        verify(processMapper).updateById(processCaptor.capture());
        assertThat(processCaptor.getValue().getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(processCaptor.getValue().getApprovedAt()).isNotNull();

        ArgumentCaptor<NoticeCreateCommand> noticeCaptor = ArgumentCaptor.forClass(NoticeCreateCommand.class);
        verify(messageCommandClient).createNotice(noticeCaptor.capture());
        assertThat(noticeCaptor.getValue().userId()).isEqualTo(APPLICANT_ID);
        assertThat(noticeCaptor.getValue().businessType()).isEqualTo(BusinessType.APPROVAL.getCode());

        ArgumentCaptor<LeaveAttendanceCommand> leaveCaptor = ArgumentCaptor.forClass(LeaveAttendanceCommand.class);
        verify(attendanceCommandClient).markLeave(leaveCaptor.capture());
        assertThat(leaveCaptor.getValue().userId()).isEqualTo(APPLICANT_ID);
        assertThat(leaveCaptor.getValue().startDate()).isEqualTo(LocalDate.of(2026, 6, 22));
        assertThat(leaveCaptor.getValue().endDate()).isEqualTo(LocalDate.of(2026, 6, 23));
        assertThat(leaveCaptor.getValue().approvalId()).isEqualTo(FORM_ID);
    }

    @Test
    void submitFailsWhenMessageServiceRejectsTodoCommand() {
        ApprovalForm form = draftForm();
        when(formMapper.selectById(FORM_ID)).thenReturn(form);
        when(formMapper.updateById(form)).thenReturn(1);
        stubApplicantEmployee();
        when(messageCommandClient.createTodo(any(TodoCreateCommand.class)))
                .thenReturn(Result.fail(500, "message unavailable"));

        assertThatThrownBy(() -> approvalService.submit(APPLICANT_ID, FORM_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("create approval todo failed");

        verify(messageCommandClient, never()).createNotice(any(NoticeCreateCommand.class));
    }

    @Test
    void highExpenseApprovalMovesFromLeaderToFinance() {
        ApprovalForm form = highExpensePendingForm();
        ApprovalProcess process = pendingProcess();
        when(formMapper.selectById(FORM_ID)).thenReturn(form);
        when(formMapper.updateById(form)).thenReturn(1);
        when(systemUserClient.getFirstUserByRole("FINANCE"))
                .thenReturn(Result.success(new CurrentUserDTO(FINANCE_ID, "finance", "Finance", 4L, 2L)));
        stubApplicantEmployee();
        stubDetailLookupsWithFinance();
        when(processMapper.selectOne(any(Wrapper.class))).thenReturn(process);
        when(messageCommandClient.completeTodo(APPROVER_ID, BusinessType.APPROVAL.getCode(), FORM_ID))
                .thenReturn(Result.success());
        when(messageCommandClient.createTodo(any(TodoCreateCommand.class))).thenReturn(Result.success());
        when(messageCommandClient.createNotice(any(NoticeCreateCommand.class))).thenReturn(Result.success());

        approvalService.approve(APPROVER_ID, FORM_ID, nullComment());

        assertThat(form.getStatus()).isEqualTo(ApprovalStatus.PROCESSING);
        assertThat(form.getCurrentApproverId()).isEqualTo(FINANCE_ID);
        assertThat(form.getCompletedAt()).isNull();

        ArgumentCaptor<ApprovalProcess> processCaptor = ArgumentCaptor.forClass(ApprovalProcess.class);
        verify(processMapper).insert(processCaptor.capture());
        assertThat(processCaptor.getValue().getApproverUserId()).isEqualTo(FINANCE_ID);
        assertThat(processCaptor.getValue().getStepOrder()).isEqualTo(2);

        ArgumentCaptor<TodoCreateCommand> todoCaptor = ArgumentCaptor.forClass(TodoCreateCommand.class);
        verify(messageCommandClient).createTodo(todoCaptor.capture());
        assertThat(todoCaptor.getValue().userId()).isEqualTo(FINANCE_ID);
    }

    @Test
    void customRuleMovesApprovalToConfiguredNextApprover() {
        ApprovalForm form = pendingForm();
        ApprovalProcess process = pendingProcess();
        ApprovalRule rule = new ApprovalRule();
        rule.setApprovalType(ApprovalType.LEAVE);
        rule.setRequiredRoles("DEPARTMENT_LEADER,ROLE:FINANCE");
        rule.setStatus(1);

        when(ruleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(rule));
        when(formMapper.selectById(FORM_ID)).thenReturn(form);
        when(formMapper.updateById(form)).thenReturn(1);
        when(systemUserClient.getFirstUserByRole("FINANCE"))
                .thenReturn(Result.success(new CurrentUserDTO(FINANCE_ID, "finance", "Finance", 4L, 2L)));
        stubApplicantEmployee();
        stubDetailLookupsWithFinance();
        when(processMapper.selectOne(any(Wrapper.class))).thenReturn(process);
        when(messageCommandClient.completeTodo(APPROVER_ID, BusinessType.APPROVAL.getCode(), FORM_ID))
                .thenReturn(Result.success());
        when(messageCommandClient.createTodo(any(TodoCreateCommand.class))).thenReturn(Result.success());
        when(messageCommandClient.createNotice(any(NoticeCreateCommand.class))).thenReturn(Result.success());

        approvalService.approve(APPROVER_ID, FORM_ID, nullComment());

        assertThat(form.getStatus()).isEqualTo(ApprovalStatus.PROCESSING);
        assertThat(form.getCurrentApproverId()).isEqualTo(FINANCE_ID);
        verify(attendanceCommandClient, never()).markLeave(any(LeaveAttendanceCommand.class));

        ArgumentCaptor<ApprovalProcess> processCaptor = ArgumentCaptor.forClass(ApprovalProcess.class);
        verify(processMapper).insert(processCaptor.capture());
        assertThat(processCaptor.getValue().getApproverUserId()).isEqualTo(FINANCE_ID);
        assertThat(processCaptor.getValue().getStepOrder()).isEqualTo(2);
    }

    @Test
    void approveFailsFastWhenOptimisticLockRejectsUpdate() {
        ApprovalForm form = pendingForm();
        when(formMapper.selectById(FORM_ID)).thenReturn(form);
        when(formMapper.updateById(form)).thenReturn(0);
        stubApplicantEmployee();

        assertThatThrownBy(() -> approvalService.approve(APPROVER_ID, FORM_ID, nullComment()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("approval form was updated by another operation");

        verify(messageCommandClient, never()).completeTodo(any(), any(), any());
        verify(messageCommandClient, never()).createNotice(any(NoticeCreateCommand.class));
        verify(processMapper, never()).updateById(any(ApprovalProcess.class));
    }

    @Test
    void scanTimeoutApprovalsNotifiesCurrentApprovers() {
        ApprovalForm form = pendingForm();
        form.setSubmittedAt(LocalDateTime.now().minusHours(30));
        when(formMapper.selectList(any(Wrapper.class))).thenReturn(List.of(form));
        when(messageCommandClient.createNotice(any(NoticeCreateCommand.class))).thenReturn(Result.success());

        int count = approvalService.scanTimeoutApprovals(24);

        assertThat(count).isEqualTo(1);
        ArgumentCaptor<NoticeCreateCommand> noticeCaptor = ArgumentCaptor.forClass(NoticeCreateCommand.class);
        verify(messageCommandClient).createNotice(noticeCaptor.capture());
        assertThat(noticeCaptor.getValue().userId()).isEqualTo(APPROVER_ID);
        assertThat(noticeCaptor.getValue().title()).isEqualTo("Approval timeout reminder");
        assertThat(noticeCaptor.getValue().businessId()).isEqualTo(FORM_ID);
    }

    private ApprovalForm draftForm() {
        ApprovalForm form = new ApprovalForm();
        form.setId(FORM_ID);
        form.setApprovalType(ApprovalType.LEAVE);
        form.setTitle("Annual leave");
        form.setApplicantUserId(APPLICANT_ID);
        form.setApplicantDeptId(2L);
        form.setLeaveStartDate(LocalDate.of(2026, 6, 22));
        form.setLeaveEndDate(LocalDate.of(2026, 6, 23));
        form.setStatus(ApprovalStatus.DRAFT);
        return form;
    }

    private ApprovalForm pendingForm() {
        ApprovalForm form = draftForm();
        form.setStatus(ApprovalStatus.PENDING);
        form.setCurrentApproverId(APPROVER_ID);
        return form;
    }

    private ApprovalForm highExpensePendingForm() {
        ApprovalForm form = pendingForm();
        form.setApprovalType(ApprovalType.EXPENSE);
        form.setAmount(BigDecimal.valueOf(1200));
        return form;
    }

    private ApprovalProcess pendingProcess() {
        ApprovalProcess process = new ApprovalProcess();
        process.setId(200L);
        process.setFormId(FORM_ID);
        process.setApproverUserId(APPROVER_ID);
        process.setStepOrder(1);
        process.setStatus(ApprovalStatus.PENDING);
        return process;
    }

    private void stubApplicantEmployee() {
        when(orgEmployeeClient.getByUserId(APPLICANT_ID))
                .thenReturn(Result.success(new OrgEmployeeDTO(3L, APPLICANT_ID, 2L, "Research and Development", APPROVER_ID)));
    }

    private void stubDetailLookups(boolean includeApprover) {
        when(recordMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(systemUserClient.getById(APPLICANT_ID))
                .thenReturn(Result.success(new CurrentUserDTO(APPLICANT_ID, "employee", "Employee", 3L, 2L)));
        if (includeApprover) {
            when(systemUserClient.getById(APPROVER_ID))
                    .thenReturn(Result.success(new CurrentUserDTO(APPROVER_ID, "manager", "Department Manager", 2L, 2L)));
        }
    }

    private void stubDetailLookupsWithFinance() {
        when(recordMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(systemUserClient.getById(APPLICANT_ID))
                .thenReturn(Result.success(new CurrentUserDTO(APPLICANT_ID, "employee", "Employee", 3L, 2L)));
        when(systemUserClient.getById(FINANCE_ID))
                .thenReturn(Result.success(new CurrentUserDTO(FINANCE_ID, "finance", "Finance", 4L, 2L)));
    }

    private com.natsukaze.smartoffice.approvalservice.dto.ApprovalActionRequest nullComment() {
        return new com.natsukaze.smartoffice.approvalservice.dto.ApprovalActionRequest();
    }
}
