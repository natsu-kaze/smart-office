package com.natsukaze.smartoffice.approvalservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.natsukaze.smartoffice.approvalservice.entity.ApprovalForm;
import com.natsukaze.smartoffice.approvalservice.entity.ApprovalProcess;
import com.natsukaze.smartoffice.approvalservice.entity.ApprovalRecord;
import com.natsukaze.smartoffice.approvalservice.mapper.ApprovalFormMapper;
import com.natsukaze.smartoffice.approvalservice.mapper.ApprovalProcessMapper;
import com.natsukaze.smartoffice.approvalservice.mapper.ApprovalRecordMapper;
import com.natsukaze.smartoffice.approvalservice.vo.ApprovalFormVO;
import com.natsukaze.smartoffice.approvalservice.vo.ApprovalRecordVO;
import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.common.enums.ApprovalAction;
import com.natsukaze.smartoffice.common.enums.ApprovalStatus;
import com.natsukaze.smartoffice.common.enums.ApprovalType;
import com.natsukaze.smartoffice.common.enums.BusinessType;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private static final String FINANCE_ROLE = "FINANCE";

    private final ApprovalFormMapper formMapper;

    private final ApprovalRecordMapper recordMapper;

    private final ApprovalProcessMapper processMapper;

    private final SystemUserClient systemUserClient;

    private final OrgEmployeeClient orgEmployeeClient;

    private final MessageCommandClient messageCommandClient;

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
        Long approverId = calculateFirstApprover(form);
        ApprovalStatus fromStatus = form.getStatus();
        form.setStatus(ApprovalStatus.PENDING);
        form.setCurrentApproverId(approverId);
        form.setSubmittedAt(LocalDateTime.now());
        updateFormOrThrow(form);
        addRecord(id, ApprovalAction.SUBMIT, userId, fromStatus, ApprovalStatus.PENDING, "submit approval");
        createProcess(form.getId(), approverId, 1);
        createTodo(approverId, form);
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
        completeTodo(userId, form.getId());
        finishCurrentProcess(form.getId(), userId, ApprovalStatus.APPROVED, request.getComment());
        addRecord(id, ApprovalAction.APPROVE, userId, fromStatus, form.getStatus(), request.getComment());
        if (nextApproverId == null) {
            notifyUser(form.getApplicantUserId(), "Approval passed", form.getTitle() + " has been approved", form.getId());
        } else {
            createProcess(form.getId(), nextApproverId, 2);
            createTodo(nextApproverId, form);
        }
        return detail(id);
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

    public int scanTimeoutApprovals(int timeoutHours) {
        int hours = timeoutHours <= 0 ? 24 : timeoutHours;
        LocalDateTime deadline = LocalDateTime.now().minusHours(hours);
        List<ApprovalForm> forms = formMapper.selectList(new LambdaQueryWrapper<ApprovalForm>()
                .in(ApprovalForm::getStatus, ApprovalStatus.PENDING, ApprovalStatus.PROCESSING)
                .isNotNull(ApprovalForm::getCurrentApproverId)
                .le(ApprovalForm::getSubmittedAt, deadline));
        forms.forEach(form -> notifyUser(
                form.getCurrentApproverId(),
                "Approval timeout reminder",
                form.getTitle() + " has been pending for over " + hours + " hours",
                form.getId()));
        return forms.size();
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
    }

    private Long calculateFirstApprover(ApprovalForm form) {
        OrgEmployeeDTO employee = requireEmployee(form.getApplicantUserId());
        Long leaderId = employee.departmentLeaderUserId();
        if (leaderId == null) {
            throw new BusinessException("department leader not found");
        }
        if (needsFinanceApproval(form) && firstUserByRole(FINANCE_ROLE) == null) {
            throw new BusinessException("finance approver not found");
        }
        return leaderId;
    }

    private Long calculateNextApprover(ApprovalForm form, Long currentApproverId) {
        if (!needsFinanceApproval(form)) {
            return null;
        }
        CurrentUserDTO financeUser = firstUserByRole(FINANCE_ROLE);
        Long financeId = financeUser == null ? null : financeUser.userId();
        if (financeId == null) {
            throw new BusinessException("finance approver not found");
        }
        return financeId.equals(currentApproverId) ? null : financeId;
    }

    private boolean needsFinanceApproval(ApprovalForm form) {
        return form.getApprovalType() == ApprovalType.EXPENSE
                && form.getAmount() != null
                && form.getAmount().compareTo(BigDecimal.valueOf(1000)) > 0;
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

    private void ensureApplicant(ApprovalForm form, Long userId) {
        if (!userId.equals(form.getApplicantUserId())) {
            throw new BusinessException("only applicant can operate this form");
        }
    }

    private void ensureApprover(ApprovalForm form, Long userId) {
        if (!isActiveApproval(form.getStatus())) {
            throw new BusinessException("approval form is not pending");
        }
        if (!userId.equals(form.getCurrentApproverId())) {
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
