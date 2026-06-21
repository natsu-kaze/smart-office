package com.natsukaze.smartoffice.approvalservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.natsukaze.smartoffice.api.attendance.client.AttendanceCommandClient;
import com.natsukaze.smartoffice.api.attendance.dto.LeaveAttendanceCommand;
import com.natsukaze.smartoffice.api.message.client.MessageCommandClient;
import com.natsukaze.smartoffice.api.message.dto.NoticeCreateCommand;
import com.natsukaze.smartoffice.api.message.dto.TodoCreateCommand;
import com.natsukaze.smartoffice.api.org.client.OrgEmployeeClient;
import com.natsukaze.smartoffice.api.org.dto.OrgEmployeeDTO;
import com.natsukaze.smartoffice.api.system.client.SystemUserClient;
import com.natsukaze.smartoffice.api.system.dto.CurrentUserDTO;
import com.natsukaze.smartoffice.approvalservice.dto.ApprovalActionRequest;
import com.natsukaze.smartoffice.approvalservice.dto.ApprovalFormRequest;
import com.natsukaze.smartoffice.approvalservice.dto.ApprovalPageQuery;
import com.natsukaze.smartoffice.approvalservice.dto.ApprovalRuleRequest;
import com.natsukaze.smartoffice.approvalservice.entity.ApprovalForm;
import com.natsukaze.smartoffice.approvalservice.entity.ApprovalProcess;
import com.natsukaze.smartoffice.approvalservice.entity.ApprovalRecord;
import com.natsukaze.smartoffice.approvalservice.entity.ApprovalRule;
import com.natsukaze.smartoffice.approvalservice.mapper.ApprovalFormMapper;
import com.natsukaze.smartoffice.approvalservice.mapper.ApprovalProcessMapper;
import com.natsukaze.smartoffice.approvalservice.mapper.ApprovalRecordMapper;
import com.natsukaze.smartoffice.approvalservice.mapper.ApprovalRuleMapper;
import com.natsukaze.smartoffice.approvalservice.vo.ApprovalFormVO;
import com.natsukaze.smartoffice.approvalservice.vo.ApprovalRecordVO;
import com.natsukaze.smartoffice.approvalservice.vo.ApprovalRuleVO;
import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.common.enums.ApprovalAction;
import com.natsukaze.smartoffice.common.enums.ApprovalStatus;
import com.natsukaze.smartoffice.common.enums.ApprovalType;
import com.natsukaze.smartoffice.common.enums.BusinessType;
import com.natsukaze.smartoffice.common.enums.TimeoutAction;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private static final String FINANCE_ROLE = "FINANCE";

    private final ApprovalFormMapper formMapper;

    private final ApprovalRecordMapper recordMapper;

    private final ApprovalProcessMapper processMapper;

    private final ApprovalRuleMapper ruleMapper;

    private final SystemUserClient systemUserClient;

    private final OrgEmployeeClient orgEmployeeClient;

    private final MessageCommandClient messageCommandClient;

    private final AttendanceCommandClient attendanceCommandClient;

    @Transactional
    public ApprovalFormVO createDraft(Long userId, ApprovalFormRequest request) {
        ApprovalForm form = new ApprovalForm();
        fillForm(form, userId, request);
        form.setStatus(ApprovalStatus.DRAFT);
        formMapper.insert(form);
        addRecord(form.getId(), ApprovalAction.CREATE, userId, null, ApprovalStatus.DRAFT, "create draft");
        return detail(form.getId());
    }

    @Transactional
    public ApprovalFormVO updateDraft(Long userId, Long id, ApprovalFormRequest request) {
        ApprovalForm form = requireForm(id);
        ensureApplicant(form, userId);
        if (form.getStatus() != ApprovalStatus.DRAFT) {
            throw new BusinessException("only draft can be updated");
        }
        fillForm(form, userId, request);
        formMapper.updateById(form);
        return detail(id);
    }

    @Transactional
    public ApprovalFormVO submit(Long userId, Long id) {
        ApprovalForm form = requireForm(id);
        ensureApplicant(form, userId);
        if (form.getStatus() != ApprovalStatus.DRAFT && form.getStatus() != ApprovalStatus.WITHDRAWN) {
            throw new BusinessException("only draft or withdrawn form can be submitted");
        }
        List<List<Long>> stepGroups = approvalStepGroups(form);
        if (stepGroups.isEmpty() || stepGroups.get(0).isEmpty()) {
            throw new BusinessException("approval approver not found");
        }
        Long firstApproverId = stepGroups.get(0).get(0);
        ApprovalStatus fromStatus = form.getStatus();
        form.setStatus(ApprovalStatus.PENDING);
        form.setCurrentApproverId(firstApproverId);
        form.setSubmittedAt(LocalDateTime.now());
        updateFormOrThrow(form);
        addRecord(id, ApprovalAction.SUBMIT, userId, fromStatus, ApprovalStatus.PENDING, "submit approval");
        // Create process entries for ALL or-sign approvers in step 1
        for (Long approverId : stepGroups.get(0)) {
            createProcess(form.getId(), approverId, 1);
            createTodo(approverId, form);
        }
        return detail(id);
    }

    @Transactional
    public ApprovalFormVO approve(Long userId, Long id, ApprovalActionRequest request) {
        ApprovalForm form = requireForm(id);
        ensureApprover(form, userId);
        ApprovalStatus fromStatus = form.getStatus();
        Long nextApproverId = calculateNextApprover(form, userId);
        if (nextApproverId == null) {
            form.setStatus(ApprovalStatus.APPROVED);
            form.setCurrentApproverId(null);
            form.setCompletedAt(LocalDateTime.now());
        } else {
            form.setStatus(ApprovalStatus.PROCESSING);
            form.setCurrentApproverId(nextApproverId);
        }
        updateFormOrThrow(form);
        // Complete todos for ALL approvers in the current step
        List<List<Long>> stepGroups = approvalStepGroups(form);
        for (List<Long> group : stepGroups) {
            if (group.contains(userId)) {
                for (Long approverId : group) {
                    completeTodo(approverId, form.getId());
                    finishCurrentProcess(form.getId(), approverId, ApprovalStatus.APPROVED, request.getComment());
                }
                break;
            }
        }
        addRecord(id, ApprovalAction.APPROVE, userId, fromStatus, form.getStatus(), request.getComment());
        if (nextApproverId == null) {
            syncLeaveAttendanceIfNeeded(form);
            notifyUser(form.getApplicantUserId(), "Approval passed", form.getTitle() + " has been approved", form.getId());
        } else {
            // Create process entries for ALL or-sign approvers in the next step
            int nextStep = findStepIndex(stepGroups, nextApproverId);
            if (nextStep >= 0) {
                for (Long approverId : stepGroups.get(nextStep)) {
                    createProcess(form.getId(), approverId, nextStep + 1);
                    createTodo(approverId, form);
                }
            }
        }
        return detail(id);
    }

    private int findStepIndex(List<List<Long>> stepGroups, Long approverId) {
        for (int i = 0; i < stepGroups.size(); i++) {
            if (stepGroups.get(i).contains(approverId)) {
                return i;
            }
        }
        return -1;
    }

    @Transactional
    public ApprovalFormVO reject(Long userId, Long id, ApprovalActionRequest request) {
        ApprovalForm form = requireForm(id);
        ensureApprover(form, userId);
        ApprovalStatus fromStatus = form.getStatus();
        form.setStatus(ApprovalStatus.REJECTED);
        form.setCurrentApproverId(null);
        form.setCompletedAt(LocalDateTime.now());
        updateFormOrThrow(form);
        completeTodo(userId, form.getId());
        finishCurrentProcess(form.getId(), userId, ApprovalStatus.REJECTED, request.getComment());
        addRecord(id, ApprovalAction.REJECT, userId, fromStatus, ApprovalStatus.REJECTED, request.getComment());
        notifyUser(form.getApplicantUserId(), "Approval rejected", form.getTitle() + " has been rejected", form.getId());
        return detail(id);
    }

    @Transactional
    public ApprovalFormVO withdraw(Long userId, Long id, ApprovalActionRequest request) {
        ApprovalForm form = requireForm(id);
        ensureApplicant(form, userId);
        if (!isActiveApproval(form.getStatus())) {
            throw new BusinessException("only pending form can be withdrawn");
        }
        ApprovalStatus fromStatus = form.getStatus();
        Long approverId = form.getCurrentApproverId();
        form.setStatus(ApprovalStatus.WITHDRAWN);
        form.setCurrentApproverId(null);
        updateFormOrThrow(form);
        if (approverId != null) {
            completeTodo(approverId, form.getId());
            finishCurrentProcess(form.getId(), approverId, ApprovalStatus.WITHDRAWN, request.getComment());
        }
        addRecord(id, ApprovalAction.WITHDRAW, userId, fromStatus, ApprovalStatus.WITHDRAWN, request.getComment());
        return detail(id);
    }

    @Transactional
    public ApprovalFormVO close(Long userId, Long id, ApprovalActionRequest request) {
        ApprovalForm form = requireForm(id);
        ensureApplicant(form, userId);
        if (form.getStatus() == ApprovalStatus.APPROVED) {
            throw new BusinessException("approved form cannot be closed");
        }
        ApprovalStatus fromStatus = form.getStatus();
        Long approverId = form.getCurrentApproverId();
        form.setStatus(ApprovalStatus.CLOSED);
        form.setCurrentApproverId(null);
        form.setCompletedAt(LocalDateTime.now());
        updateFormOrThrow(form);
        if (isActiveApproval(fromStatus) && approverId != null) {
            completeTodo(approverId, form.getId());
            finishCurrentProcess(form.getId(), approverId, ApprovalStatus.CLOSED, request.getComment());
        }
        addRecord(id, ApprovalAction.CLOSE, userId, fromStatus, ApprovalStatus.CLOSED, request.getComment());
        return detail(id);
    }

    public ApprovalFormVO detail(Long id) {
        ApprovalForm form = requireForm(id);
        ApprovalFormVO vo = toVO(form);
        vo.setRecords(records(id));
        return vo;
    }

    public PageResult<ApprovalFormVO> myApplications(Long userId, ApprovalPageQuery query) {
        LambdaQueryWrapper<ApprovalForm> wrapper = baseQuery(query)
                .eq(ApprovalForm::getApplicantUserId, userId)
                .orderByDesc(ApprovalForm::getCreateTime);
        Page<ApprovalForm> page = formMapper.selectPage(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.from(page.convert(this::toVO));
    }

    public PageResult<ApprovalFormVO> myTodos(Long userId, ApprovalPageQuery query) {
        LambdaQueryWrapper<ApprovalForm> wrapper = baseQuery(query)
                .eq(ApprovalForm::getCurrentApproverId, userId)
                .in(ApprovalForm::getStatus, ApprovalStatus.PENDING, ApprovalStatus.PROCESSING)
                .orderByDesc(ApprovalForm::getSubmittedAt);
        Page<ApprovalForm> page = formMapper.selectPage(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.from(page.convert(this::toVO));
    }

    public List<ApprovalRecordVO> records(Long id) {
        return recordMapper.selectList(new LambdaQueryWrapper<ApprovalRecord>()
                        .eq(ApprovalRecord::getFormId, id)
                        .orderByAsc(ApprovalRecord::getCreateTime))
                .stream()
                .map(this::toRecordVO)
                .toList();
    }

    /**
     * Scan pending/processing approvals for timeout.
     * For forms with a matched rule that has timeoutHours + timeoutAction configured,
     * execute the timeout action (auto-approve, auto-reject, or escalate).
     * Otherwise, fall back to sending reminder notifications.
     *
     * @param defaultTimeoutHours the fallback threshold for notification-only rules (default 24)
     * @return count of forms processed
     */
    public int scanTimeoutApprovals(int defaultTimeoutHours) {
        int defaultHours = defaultTimeoutHours <= 0 ? 24 : defaultTimeoutHours;
        LocalDateTime defaultDeadline = LocalDateTime.now().minusHours(defaultHours);

        List<ApprovalForm> forms = formMapper.selectList(new LambdaQueryWrapper<ApprovalForm>()
                .in(ApprovalForm::getStatus, ApprovalStatus.PENDING, ApprovalStatus.PROCESSING)
                .isNotNull(ApprovalForm::getCurrentApproverId));

        int processed = 0;
        for (ApprovalForm form : forms) {
            ApprovalRule rule = matchedRule(form);
            Integer ruleTimeoutHours = rule != null ? rule.getTimeoutHours() : null;
            TimeoutAction action = rule != null ? rule.getTimeoutAction() : null;

            // Rule-level timeout: check if form has been pending longer than rule's timeoutHours
            if (ruleTimeoutHours != null && action != null && form.getSubmittedAt() != null) {
                LocalDateTime ruleDeadline = form.getSubmittedAt().plusHours(ruleTimeoutHours);
                if (LocalDateTime.now().isAfter(ruleDeadline)) {
                    executeTimeoutAction(form, action);
                    processed++;
                    continue;
                }
            }

            // Fallback: send reminder for forms overdue by the default threshold
            if (form.getSubmittedAt() != null && form.getSubmittedAt().isBefore(defaultDeadline)) {
                notifyUser(
                        form.getCurrentApproverId(),
                        "Approval timeout reminder",
                        form.getTitle() + " has been pending for over " + defaultHours + " hours",
                        form.getId());
                processed++;
            }
        }
        return processed;
    }

    private void executeTimeoutAction(ApprovalForm form, TimeoutAction action) {
        Long systemUserId = 0L;
        ApprovalStatus fromStatus = form.getStatus();
        Long oldApproverId = form.getCurrentApproverId(); // capture before mutation
        ApprovalRule rule = matchedRule(form);
        int timeoutHoursVal = rule != null && rule.getTimeoutHours() != null ? rule.getTimeoutHours() : 0;

        switch (action) {
            case AUTO_APPROVE -> {
                Long nextApproverId = calculateNextApprover(form, oldApproverId);
                if (nextApproverId == null) {
                    form.setStatus(ApprovalStatus.APPROVED);
                    form.setCurrentApproverId(null);
                    form.setCompletedAt(LocalDateTime.now());
                } else {
                    form.setStatus(ApprovalStatus.PROCESSING);
                    form.setCurrentApproverId(nextApproverId);
                }
                updateFormOrThrow(form);
                completeStepTodosAndProcesses(form, oldApproverId);
                addRecord(form.getId(), ApprovalAction.APPROVE, systemUserId, fromStatus, form.getStatus(),
                        "Auto-approved: timeout after " + timeoutHoursVal + " hours");
                if (nextApproverId == null) {
                    syncLeaveAttendanceIfNeeded(form);
                } else {
                    List<List<Long>> stepGroups = approvalStepGroups(form);
                    int nextStep = findStepIndex(stepGroups, nextApproverId);
                    if (nextStep >= 0) {
                        for (Long approverId : stepGroups.get(nextStep)) {
                            createProcess(form.getId(), approverId, nextStep + 1);
                            createTodo(approverId, form);
                        }
                    }
                }
                notifyUser(form.getApplicantUserId(), "Approval auto-passed (timeout)",
                        form.getTitle() + " was auto-approved due to timeout", form.getId());
            }
            case AUTO_REJECT -> {
                form.setStatus(ApprovalStatus.REJECTED);
                form.setCurrentApproverId(null);
                form.setCompletedAt(LocalDateTime.now());
                updateFormOrThrow(form);
                completeStepTodosAndProcesses(form, oldApproverId);
                addRecord(form.getId(), ApprovalAction.REJECT, systemUserId, fromStatus, form.getStatus(),
                        "Auto-rejected: timeout after " + timeoutHoursVal + " hours");
                notifyUser(form.getApplicantUserId(), "Approval auto-rejected (timeout)",
                        form.getTitle() + " was auto-rejected due to timeout", form.getId());
            }
            case ESCALATE -> {
                Long nextApproverId = calculateNextApprover(form, oldApproverId);
                if (nextApproverId == null) {
                    form.setStatus(ApprovalStatus.APPROVED);
                    form.setCurrentApproverId(null);
                    form.setCompletedAt(LocalDateTime.now());
                } else {
                    form.setStatus(ApprovalStatus.PROCESSING);
                    form.setCurrentApproverId(nextApproverId);
                }
                updateFormOrThrow(form);
                completeStepTodosAndProcesses(form, oldApproverId);
                addRecord(form.getId(), ApprovalAction.APPROVE, systemUserId, fromStatus, form.getStatus(),
                        "Escalated: timeout, moved to next step");
                if (nextApproverId == null) {
                    syncLeaveAttendanceIfNeeded(form);
                } else {
                    List<List<Long>> stepGroups = approvalStepGroups(form);
                    int nextStep = findStepIndex(stepGroups, nextApproverId);
                    if (nextStep >= 0) {
                        for (Long approverId : stepGroups.get(nextStep)) {
                            createProcess(form.getId(), approverId, nextStep + 1);
                            createTodo(approverId, form);
                        }
                    }
                }
                notifyUser(form.getApplicantUserId(), "Approval escalated (timeout)",
                        form.getTitle() + " was escalated due to timeout", form.getId());
            }
        }
    }

    /**
     * Complete todos and finish process records for all approvers in the given user's current step.
     */
    private void completeStepTodosAndProcesses(ApprovalForm form, Long userIdInStep) {
        List<List<Long>> stepGroups = approvalStepGroups(form);
        for (List<Long> group : stepGroups) {
            if (group.contains(userIdInStep)) {
                for (Long approverId : group) {
                    completeTodo(approverId, form.getId());
                    finishCurrentProcess(form.getId(), approverId, ApprovalStatus.APPROVED, "timeout action");
                }
                break;
            }
        }
    }

    public List<ApprovalRuleVO> listRules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<ApprovalRule>()
                        .orderByAsc(ApprovalRule::getApprovalType)
                        .orderByDesc(ApprovalRule::getPriority)
                        .orderByAsc(ApprovalRule::getId))
                .stream()
                .map(this::toRuleVO)
                .toList();
    }

    @Transactional
    public ApprovalRuleVO saveRule(ApprovalRuleRequest request) {
        ApprovalRule rule = new ApprovalRule();
        fillRule(rule, request);
        ruleMapper.insert(rule);
        return toRuleVO(rule);
    }

    @Transactional
    public ApprovalRuleVO updateRule(Long id, ApprovalRuleRequest request) {
        ApprovalRule rule = requireRule(id);
        fillRule(rule, request);
        ruleMapper.updateById(rule);
        return toRuleVO(rule);
    }

    @Transactional
    public void deleteDraft(Long userId, Long id) {
        ApprovalForm form = requireForm(id);
        ensureApplicant(form, userId);
        if (form.getStatus() != ApprovalStatus.DRAFT) {
            throw new BusinessException("only draft can be deleted");
        }
        addRecord(id, ApprovalAction.DELETE, userId, ApprovalStatus.DRAFT, ApprovalStatus.DRAFT, "delete draft");
        processMapper.physicalDeleteByFormId(id);
        recordMapper.delete(new LambdaQueryWrapper<ApprovalRecord>().eq(ApprovalRecord::getFormId, id));
        if (formMapper.deleteById(id) != 1) {
            throw new BusinessException("approval form not found");
        }
    }

    @Transactional
    public void deleteRule(Long id) {
        if (ruleMapper.deleteById(id) != 1) {
            throw new BusinessException("approval rule not found");
        }
    }

    private LambdaQueryWrapper<ApprovalForm> baseQuery(ApprovalPageQuery query) {
        return new LambdaQueryWrapper<ApprovalForm>()
                .eq(StringUtils.hasText(query.getApprovalType()), ApprovalForm::getApprovalType,
                        StringUtils.hasText(query.getApprovalType()) ? ApprovalType.of(query.getApprovalType()) : null)
                .eq(StringUtils.hasText(query.getStatus()), ApprovalForm::getStatus,
                        StringUtils.hasText(query.getStatus()) ? ApprovalStatus.of(query.getStatus()) : null)
                .like(StringUtils.hasText(query.getKeyword()), ApprovalForm::getTitle, query.getKeyword());
    }

    private void fillForm(ApprovalForm form, Long userId, ApprovalFormRequest request) {
        OrgEmployeeDTO employee = requireEmployee(userId);
        form.setApprovalType(ApprovalType.of(request.getApprovalType()));
        form.setTitle(request.getTitle());
        form.setApplicantUserId(userId);
        form.setApplicantDeptId(employee.departmentId());
        form.setContent(request.getContent());
        form.setAmount(request.getAmount());
        form.setLeaveStartDate(request.getLeaveStartDate());
        form.setLeaveEndDate(request.getLeaveEndDate());
        if (form.getApprovalType() == ApprovalType.LEAVE) {
            validateLeaveDates(form);
        }
    }

    private void validateLeaveDates(ApprovalForm form) {
        if (form.getLeaveStartDate() == null || form.getLeaveEndDate() == null) {
            throw new BusinessException("leave date range is required");
        }
        if (form.getLeaveEndDate().isBefore(form.getLeaveStartDate())) {
            throw new BusinessException("leave end date cannot be before start date");
        }
    }

    private void syncLeaveAttendanceIfNeeded(ApprovalForm form) {
        if (form.getApprovalType() != ApprovalType.LEAVE) {
            return;
        }
        requireSuccess(attendanceCommandClient.markLeave(new LeaveAttendanceCommand(
                form.getApplicantUserId(),
                form.getLeaveStartDate(),
                form.getLeaveEndDate(),
                form.getId()
        )), "sync leave attendance failed");
    }

    private Long calculateFirstApprover(ApprovalForm form) {
        List<List<Long>> stepGroups = approvalStepGroups(form);
        if (stepGroups.isEmpty() || stepGroups.get(0).isEmpty()) {
            throw new BusinessException("approval approver not found");
        }
        return stepGroups.get(0).get(0);
    }

    private Long calculateNextApprover(ApprovalForm form, Long currentApproverId) {
        List<List<Long>> stepGroups = approvalStepGroups(form);
        for (int i = 0; i < stepGroups.size(); i++) {
            if (stepGroups.get(i).contains(currentApproverId)) {
                if (i + 1 >= stepGroups.size()) {
                    return null;
                }
                List<Long> nextGroup = stepGroups.get(i + 1);
                return nextGroup.isEmpty() ? null : nextGroup.get(0);
            }
        }
        return null;
    }

    private boolean needsFinanceApproval(ApprovalForm form) {
        return form.getApprovalType() == ApprovalType.EXPENSE
                && form.getAmount() != null
                && form.getAmount().compareTo(BigDecimal.valueOf(1000)) > 0;
    }

    /**
     * Parse requiredRoles into step groups.
     * Comma separates sequential steps.
     * Pipe separates or-sign approvers within a step (any one can approve).
     * Example: "DEPARTMENT_LEADER|ROLE:HR,ROLE:FINANCE" means
     *   step 1: dept leader OR HR (either can approve)
     *   step 2: finance (required)
     */
    private List<List<Long>> approvalStepGroups(ApprovalForm form) {
        ApprovalRule rule = matchedRule(form);
        if (rule == null || !StringUtils.hasText(rule.getRequiredRoles())) {
            return defaultApprovalStepGroups(form);
        }
        return resolveApproverStepGroups(form, rule.getRequiredRoles());
    }

    private ApprovalRule matchedRule(ApprovalForm form) {
        List<ApprovalRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<ApprovalRule>()
                .eq(ApprovalRule::getApprovalType, form.getApprovalType())
                .eq(ApprovalRule::getStatus, 1));
        if (rules.isEmpty()) {
            return null;
        }
        BigDecimal amount = form.getAmount() == null ? BigDecimal.ZERO : form.getAmount();
        return rules.stream()
                // Dept scope: null=global, or matches applicant's deptId
                .filter(rule -> rule.getDeptId() == null
                        || rule.getDeptId().equals(form.getApplicantDeptId()))
                // Amount threshold: form amount >= rule amountLimit
                .filter(rule -> rule.getAmountLimit() == null
                        || amount.compareTo(rule.getAmountLimit()) >= 0)
                // Applicant role filter: null=any, or applicant must have the role
                .filter(rule -> rule.getApplicantRoleCode() == null
                        || rule.getApplicantRoleCode().isBlank()
                        || applicantHasRole(form.getApplicantUserId(), rule.getApplicantRoleCode()))
                // Highest priority wins, tie-break by oldest rule (lowest id)
                .max(Comparator.comparingInt((ApprovalRule r) ->
                                r.getPriority() == null ? 0 : r.getPriority())
                        .thenComparing(Comparator.comparingLong(ApprovalRule::getId).reversed()))
                .orElse(null);
    }

    private boolean applicantHasRole(Long userId, String roleCode) {
        try {
            Result<Boolean> result = systemUserClient.hasRole(userId, roleCode);
            return success(result) && Boolean.TRUE.equals(result.data());
        } catch (Exception e) {
            return false;
        }
    }

    private List<List<Long>> defaultApprovalStepGroups(ApprovalForm form) {
        List<List<Long>> steps = new ArrayList<>();
        OrgEmployeeDTO employee = requireEmployee(form.getApplicantUserId());
        if (employee.departmentLeaderUserId() != null) {
            steps.add(new ArrayList<>(List.of(employee.departmentLeaderUserId())));
        }
        if (needsFinanceApproval(form)) {
            CurrentUserDTO financeUser = firstUserByRole(FINANCE_ROLE);
            if (financeUser == null || financeUser.userId() == null) {
                throw new BusinessException("finance approver not found");
            }
            steps.add(new ArrayList<>(List.of(financeUser.userId())));
        }
        return steps;
    }

    /**
     * Parse comma-separated steps, each step may contain pipe-separated or-sign approvers.
     */
    private List<List<Long>> resolveApproverStepGroups(ApprovalForm form, String config) {
        List<List<Long>> steps = new ArrayList<>();
        for (String stepToken : config.split(",")) {
            String trimmed = stepToken.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            List<Long> group = new ArrayList<>();
            for (String orToken : trimmed.split("\\|")) {
                String orTrimmed = orToken.trim();
                if (!StringUtils.hasText(orTrimmed)) {
                    continue;
                }
                Long approverId = resolveApproverToken(form, orTrimmed);
                if (approverId != null) {
                    group.add(approverId);
                }
            }
            if (!group.isEmpty()) {
                steps.add(distinctApprovers(group));
            }
        }
        return steps;
    }

    private Long resolveApproverToken(ApprovalForm form, String token) {
        String normalized = token.trim().toUpperCase(Locale.ROOT);
        if ("DEPARTMENT_LEADER".equals(normalized) || "LEADER".equals(normalized)) {
            Long leaderId = requireEmployee(form.getApplicantUserId()).departmentLeaderUserId();
            if (leaderId == null) {
                throw new BusinessException("department leader not found");
            }
            return leaderId;
        }
        if (normalized.startsWith("USER:")) {
            try {
                Long userId = Long.valueOf(token.substring("USER:".length()).trim());
                if (safeUser(userId) == null) {
                    throw new BusinessException("approval user not found: " + userId);
                }
                return userId;
            } catch (NumberFormatException ex) {
                throw new BusinessException("invalid approval user token: " + token);
            }
        }
        String roleCode = normalized.startsWith("ROLE:") ? token.substring("ROLE:".length()).trim() : token.trim();
        CurrentUserDTO user = firstUserByRole(roleCode);
        if (user == null || user.userId() == null) {
            throw new BusinessException("approver role has no user: " + roleCode);
        }
        return user.userId();
    }

    private List<Long> distinctApprovers(List<Long> approvers) {
        Set<Long> deduplicated = new LinkedHashSet<>(approvers);
        return new ArrayList<>(deduplicated);
    }

    private void createTodo(Long userId, ApprovalForm form) {
        requireSuccess(messageCommandClient.createTodo(new TodoCreateCommand(
                userId,
                "Approval todo: " + form.getTitle(),
                BusinessType.APPROVAL.getCode(),
                form.getId(),
                null)), "create approval todo failed");
        notifyUser(userId, "New approval todo", form.getTitle(), form.getId());
    }

    private void completeTodo(Long userId, Long formId) {
        requireSuccess(messageCommandClient.completeTodo(userId, BusinessType.APPROVAL.getCode(), formId),
                "complete approval todo failed");
    }

    private void notifyUser(Long userId, String title, String content, Long formId) {
        requireSuccess(messageCommandClient.createNotice(new NoticeCreateCommand(
                userId,
                title,
                content,
                BusinessType.APPROVAL.getCode(),
                formId)), "create approval notice failed");
    }

    private void createProcess(Long formId, Long approverId, int stepOrder) {
        ApprovalProcess process = new ApprovalProcess();
        process.setFormId(formId);
        process.setApproverUserId(approverId);
        process.setStepOrder(stepOrder);
        process.setStatus(ApprovalStatus.PENDING);
        processMapper.insert(process);
    }

    private int nextStepOrder(Long formId) {
        ApprovalProcess last = processMapper.selectOne(new LambdaQueryWrapper<ApprovalProcess>()
                .eq(ApprovalProcess::getFormId, formId)
                .orderByDesc(ApprovalProcess::getStepOrder)
                .last("LIMIT 1"));
        return last == null || last.getStepOrder() == null ? 1 : last.getStepOrder() + 1;
    }

    private void finishCurrentProcess(Long formId, Long approverId, ApprovalStatus status, String comment) {
        ApprovalProcess process = processMapper.selectOne(new LambdaQueryWrapper<ApprovalProcess>()
                .eq(ApprovalProcess::getFormId, formId)
                .eq(ApprovalProcess::getApproverUserId, approverId)
                .eq(ApprovalProcess::getStatus, ApprovalStatus.PENDING)
                .last("LIMIT 1"));
        if (process != null) {
            process.setStatus(status);
            process.setApprovedAt(LocalDateTime.now());
            process.setComment(comment);
            processMapper.updateById(process);
        }
    }

    private void requireSuccess(Result<?> result, String message) {
        if (!success(result)) {
            throw new BusinessException(message);
        }
    }

    private void addRecord(Long formId, ApprovalAction action, Long operatorUserId, ApprovalStatus fromStatus,
                           ApprovalStatus toStatus, String comment) {
        ApprovalRecord record = new ApprovalRecord();
        record.setFormId(formId);
        record.setAction(action);
        record.setOperatorUserId(operatorUserId);
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setComment(comment);
        recordMapper.insert(record);
    }

    private ApprovalForm requireForm(Long id) {
        ApprovalForm form = formMapper.selectById(id);
        if (form == null) {
            throw new BusinessException("approval form not found");
        }
        return form;
    }

    private ApprovalRule requireRule(Long id) {
        ApprovalRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException("approval rule not found");
        }
        return rule;
    }

    private void ensureApplicant(ApprovalForm form, Long userId) {
        if (!userId.equals(form.getApplicantUserId())) {
            throw new BusinessException("only applicant can operate this form");
        }
    }

    private void ensureApprover(ApprovalForm form, Long userId) {
        if (!isActiveApproval(form.getStatus())) {
            throw new BusinessException("approval form is not pending");
        }
        // Or-sign: check if userId is in the current step's approver group
        List<List<Long>> stepGroups = approvalStepGroups(form);
        boolean found = false;
        for (List<Long> group : stepGroups) {
            if (group.contains(form.getCurrentApproverId())) {
                found = group.contains(userId);
                break;
            }
        }
        if (!found) {
            throw new BusinessException("current user is not approver");
        }
    }

    private void updateFormOrThrow(ApprovalForm form) {
        if (formMapper.updateById(form) != 1) {
            throw new BusinessException("approval form was updated by another operation");
        }
    }

    private boolean isActiveApproval(ApprovalStatus status) {
        return status == ApprovalStatus.PENDING || status == ApprovalStatus.PROCESSING;
    }

    private ApprovalFormVO toVO(ApprovalForm form) {
        CurrentUserDTO applicant = safeUser(form.getApplicantUserId());
        CurrentUserDTO approver = form.getCurrentApproverId() == null ? null : safeUser(form.getCurrentApproverId());
        OrgEmployeeDTO employee = safeEmployee(form.getApplicantUserId());
        return ApprovalFormVO.builder()
                .id(form.getId())
                .approvalType(form.getApprovalType().getCode())
                .title(form.getTitle())
                .applicantUserId(form.getApplicantUserId())
                .applicantName(realName(applicant))
                .applicantDeptId(form.getApplicantDeptId())
                .applicantDeptName(employee == null ? null : employee.departmentName())
                .content(form.getContent())
                .amount(form.getAmount())
                .leaveStartDate(form.getLeaveStartDate())
                .leaveEndDate(form.getLeaveEndDate())
                .status(form.getStatus().getCode())
                .currentApproverId(form.getCurrentApproverId())
                .currentApproverName(realName(approver))
                .submittedAt(form.getSubmittedAt())
                .completedAt(form.getCompletedAt())
                .build();
    }

    private ApprovalRecordVO toRecordVO(ApprovalRecord record) {
        CurrentUserDTO operator = safeUser(record.getOperatorUserId());
        return ApprovalRecordVO.builder()
                .id(record.getId())
                .formId(record.getFormId())
                .action(record.getAction().getCode())
                .operatorUserId(record.getOperatorUserId())
                .operatorName(realName(operator))
                .fromStatus(record.getFromStatus() == null ? null : record.getFromStatus().getCode())
                .toStatus(record.getToStatus().getCode())
                .comment(record.getComment())
                .createTime(record.getCreateTime())
                .build();
    }

    private void fillRule(ApprovalRule rule, ApprovalRuleRequest request) {
        rule.setApprovalType(ApprovalType.of(request.getApprovalType()));
        rule.setName(request.getName());
        rule.setPriority(request.getPriority() == null ? 0 : request.getPriority());
        rule.setAmountLimit(request.getAmountLimit());
        rule.setApplicantRoleCode(request.getApplicantRoleCode());
        rule.setDeptId(request.getDeptId());
        rule.setRequiredRoles(request.getRequiredRoles());
        rule.setTimeoutHours(request.getTimeoutHours());
        rule.setTimeoutAction(TimeoutAction.ofNullable(request.getTimeoutAction()));
        rule.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        rule.setRemark(request.getRemark());
    }

    private ApprovalRuleVO toRuleVO(ApprovalRule rule) {
        return ApprovalRuleVO.builder()
                .id(rule.getId())
                .approvalType(rule.getApprovalType().getCode())
                .name(rule.getName())
                .priority(rule.getPriority())
                .amountLimit(rule.getAmountLimit())
                .applicantRoleCode(rule.getApplicantRoleCode())
                .deptId(rule.getDeptId())
                .requiredRoles(rule.getRequiredRoles())
                .timeoutHours(rule.getTimeoutHours())
                .timeoutAction(rule.getTimeoutAction() == null ? null : rule.getTimeoutAction().getCode())
                .status(rule.getStatus())
                .remark(rule.getRemark())
                .build();
    }

    private OrgEmployeeDTO requireEmployee(Long userId) {
        OrgEmployeeDTO employee = safeEmployee(userId);
        if (employee == null) {
            throw new BusinessException("employee not found");
        }
        return employee;
    }

    private OrgEmployeeDTO safeEmployee(Long userId) {
        Result<OrgEmployeeDTO> result = orgEmployeeClient.getByUserId(userId);
        return success(result) ? result.data() : null;
    }

    private CurrentUserDTO safeUser(Long userId) {
        Result<CurrentUserDTO> result = systemUserClient.getById(userId);
        return success(result) ? result.data() : null;
    }

    private CurrentUserDTO firstUserByRole(String roleCode) {
        Result<CurrentUserDTO> result = systemUserClient.getFirstUserByRole(roleCode);
        return success(result) ? result.data() : null;
    }

    private boolean success(Result<?> result) {
        return result != null && result.code() == ErrorCode.SUCCESS.getCode();
    }

    private String realName(CurrentUserDTO user) {
        return user == null ? null : user.realName();
    }
}
