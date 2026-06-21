package com.natsukaze.smartoffice.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.natsukaze.smartoffice.approval.dto.ApprovalActionRequest;
import com.natsukaze.smartoffice.approval.dto.ApprovalFormRequest;
import com.natsukaze.smartoffice.approval.dto.ApprovalPageQuery;
import com.natsukaze.smartoffice.approval.entity.ApprovalForm;
import com.natsukaze.smartoffice.approval.entity.ApprovalRecord;
import com.natsukaze.smartoffice.approval.mapper.ApprovalFormMapper;
import com.natsukaze.smartoffice.approval.mapper.ApprovalRecordMapper;
import com.natsukaze.smartoffice.approval.vo.ApprovalFormVO;
import com.natsukaze.smartoffice.approval.vo.ApprovalRecordVO;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.enums.ApprovalAction;
import com.natsukaze.smartoffice.common.enums.ApprovalStatus;
import com.natsukaze.smartoffice.common.enums.ApprovalType;
import com.natsukaze.smartoffice.common.enums.BusinessType;
import com.natsukaze.smartoffice.common.enums.TodoStatus;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import com.natsukaze.smartoffice.message.entity.MessageNotice;
import com.natsukaze.smartoffice.message.entity.MessageTodo;
import com.natsukaze.smartoffice.message.mapper.MessageNoticeMapper;
import com.natsukaze.smartoffice.message.mapper.MessageTodoMapper;
import com.natsukaze.smartoffice.org.entity.OrgDepartment;
import com.natsukaze.smartoffice.org.entity.OrgEmployee;
import com.natsukaze.smartoffice.org.mapper.OrgDepartmentMapper;
import com.natsukaze.smartoffice.org.mapper.OrgEmployeeMapper;
import com.natsukaze.smartoffice.user.entity.SysRole;
import com.natsukaze.smartoffice.user.entity.SysUser;
import com.natsukaze.smartoffice.user.entity.SysUserRole;
import com.natsukaze.smartoffice.user.mapper.SysRoleMapper;
import com.natsukaze.smartoffice.user.mapper.SysUserMapper;
import com.natsukaze.smartoffice.user.mapper.SysUserRoleMapper;
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

    private final ApprovalFormMapper formMapper;
    private final ApprovalRecordMapper recordMapper;
    private final MessageTodoMapper todoMapper;
    private final MessageNoticeMapper noticeMapper;
    private final OrgEmployeeMapper employeeMapper;
    private final OrgDepartmentMapper departmentMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

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
        formMapper.updateById(form);
        addRecord(id, ApprovalAction.SUBMIT, userId, fromStatus, ApprovalStatus.PENDING, "submit approval");
        createTodo(approverId, form);
        return detail(id);
    }

    @Transactional
    public ApprovalFormVO approve(Long userId, Long id, ApprovalActionRequest request) {
        ApprovalForm form = requireForm(id);
        ensureApprover(form, userId);
        form.setStatus(ApprovalStatus.APPROVED);
        form.setCurrentApproverId(null);
        form.setCompletedAt(LocalDateTime.now());
        formMapper.updateById(form);
        completeTodo(userId, form.getId());
        addRecord(id, ApprovalAction.APPROVE, userId, ApprovalStatus.PENDING, ApprovalStatus.APPROVED, request.getComment());
        notifyUser(form.getApplicantUserId(), "Approval passed", form.getTitle() + " has been approved", form.getId());
        return detail(id);
    }

    @Transactional
    public ApprovalFormVO reject(Long userId, Long id, ApprovalActionRequest request) {
        ApprovalForm form = requireForm(id);
        ensureApprover(form, userId);
        form.setStatus(ApprovalStatus.REJECTED);
        form.setCurrentApproverId(null);
        form.setCompletedAt(LocalDateTime.now());
        formMapper.updateById(form);
        completeTodo(userId, form.getId());
        addRecord(id, ApprovalAction.REJECT, userId, ApprovalStatus.PENDING, ApprovalStatus.REJECTED, request.getComment());
        notifyUser(form.getApplicantUserId(), "Approval rejected", form.getTitle() + " has been rejected", form.getId());
        return detail(id);
    }

    @Transactional
    public ApprovalFormVO withdraw(Long userId, Long id, ApprovalActionRequest request) {
        ApprovalForm form = requireForm(id);
        ensureApplicant(form, userId);
        if (form.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException("only pending form can be withdrawn");
        }
        Long approverId = form.getCurrentApproverId();
        form.setStatus(ApprovalStatus.WITHDRAWN);
        form.setCurrentApproverId(null);
        formMapper.updateById(form);
        if (approverId != null) {
            completeTodo(approverId, form.getId());
        }
        addRecord(id, ApprovalAction.WITHDRAW, userId, ApprovalStatus.PENDING, ApprovalStatus.WITHDRAWN, request.getComment());
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
        form.setStatus(ApprovalStatus.CLOSED);
        form.setCurrentApproverId(null);
        form.setCompletedAt(LocalDateTime.now());
        formMapper.updateById(form);
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
                .eq(ApprovalForm::getStatus, ApprovalStatus.PENDING)
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

    private LambdaQueryWrapper<ApprovalForm> baseQuery(ApprovalPageQuery query) {
        return new LambdaQueryWrapper<ApprovalForm>()
                .eq(StringUtils.hasText(query.getApprovalType()), ApprovalForm::getApprovalType,
                        StringUtils.hasText(query.getApprovalType()) ? ApprovalType.of(query.getApprovalType()) : null)
                .eq(StringUtils.hasText(query.getStatus()), ApprovalForm::getStatus,
                        StringUtils.hasText(query.getStatus()) ? ApprovalStatus.of(query.getStatus()) : null)
                .like(StringUtils.hasText(query.getKeyword()), ApprovalForm::getTitle, query.getKeyword());
    }

    private void fillForm(ApprovalForm form, Long userId, ApprovalFormRequest request) {
        OrgEmployee employee = employeeMapper.selectOne(new LambdaQueryWrapper<OrgEmployee>()
                .eq(OrgEmployee::getUserId, userId)
                .last("LIMIT 1"));
        form.setApprovalType(ApprovalType.of(request.getApprovalType()));
        form.setTitle(request.getTitle());
        form.setApplicantUserId(userId);
        form.setApplicantDeptId(employee == null ? null : employee.getDepartmentId());
        form.setContent(request.getContent());
        form.setAmount(request.getAmount());
        form.setLeaveStartDate(request.getLeaveStartDate());
        form.setLeaveEndDate(request.getLeaveEndDate());
    }

    private Long calculateFirstApprover(ApprovalForm form) {
        OrgDepartment department = form.getApplicantDeptId() == null ? null : departmentMapper.selectById(form.getApplicantDeptId());
        Long leaderId = department == null ? null : department.getLeaderUserId();
        if (form.getApprovalType() == ApprovalType.EXPENSE
                && form.getAmount() != null
                && form.getAmount().compareTo(BigDecimal.valueOf(1000)) > 0) {
            Long financeId = firstUserByRole("FINANCE");
            if (leaderId == null && financeId == null) {
                throw new BusinessException("approval approver not found");
            }
            return leaderId == null ? financeId : leaderId;
        }
        if (leaderId == null) {
            throw new BusinessException("department leader not found");
        }
        return leaderId;
    }

    private Long firstUserByRole(String roleCode) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode)
                .last("LIMIT 1"));
        if (role == null) {
            return null;
        }
        SysUserRole relation = userRoleMapper.selectOne(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, role.getId())
                .last("LIMIT 1"));
        return relation == null ? null : relation.getUserId();
    }

    private void createTodo(Long userId, ApprovalForm form) {
        MessageTodo todo = new MessageTodo();
        todo.setUserId(userId);
        todo.setTitle("Approval todo: " + form.getTitle());
        todo.setBusinessType(BusinessType.APPROVAL);
        todo.setBusinessId(form.getId());
        todo.setStatus(TodoStatus.PENDING);
        todoMapper.insert(todo);
        notifyUser(userId, "New approval todo", form.getTitle(), form.getId());
    }

    private void completeTodo(Long userId, Long formId) {
        MessageTodo todo = todoMapper.selectOne(new LambdaQueryWrapper<MessageTodo>()
                .eq(MessageTodo::getUserId, userId)
                .eq(MessageTodo::getBusinessType, BusinessType.APPROVAL)
                .eq(MessageTodo::getBusinessId, formId)
                .eq(MessageTodo::getStatus, TodoStatus.PENDING)
                .last("LIMIT 1"));
        if (todo != null) {
            todo.setStatus(TodoStatus.DONE);
            todo.setCompletedTime(LocalDateTime.now());
            todoMapper.updateById(todo);
        }
    }

    private void notifyUser(Long userId, String title, String content, Long formId) {
        MessageNotice notice = new MessageNotice();
        notice.setUserId(userId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setBusinessType(BusinessType.APPROVAL);
        notice.setBusinessId(formId);
        notice.setReadStatus(0);
        noticeMapper.insert(notice);
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
        if (form.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException("approval form is not pending");
        }
        if (!userId.equals(form.getCurrentApproverId())) {
            throw new BusinessException("current user is not approver");
        }
    }

    private ApprovalFormVO toVO(ApprovalForm form) {
        SysUser applicant = userMapper.selectById(form.getApplicantUserId());
        SysUser approver = form.getCurrentApproverId() == null ? null : userMapper.selectById(form.getCurrentApproverId());
        OrgDepartment department = form.getApplicantDeptId() == null ? null : departmentMapper.selectById(form.getApplicantDeptId());
        return ApprovalFormVO.builder()
                .id(form.getId())
                .approvalType(form.getApprovalType().getCode())
                .title(form.getTitle())
                .applicantUserId(form.getApplicantUserId())
                .applicantName(applicant == null ? null : applicant.getRealName())
                .applicantDeptId(form.getApplicantDeptId())
                .applicantDeptName(department == null ? null : department.getDepartmentName())
                .content(form.getContent())
                .amount(form.getAmount())
                .status(form.getStatus().getCode())
                .currentApproverId(form.getCurrentApproverId())
                .currentApproverName(approver == null ? null : approver.getRealName())
                .submittedAt(form.getSubmittedAt())
                .completedAt(form.getCompletedAt())
                .build();
    }

    private ApprovalRecordVO toRecordVO(ApprovalRecord record) {
        SysUser operator = userMapper.selectById(record.getOperatorUserId());
        return ApprovalRecordVO.builder()
                .id(record.getId())
                .formId(record.getFormId())
                .action(record.getAction().getCode())
                .operatorUserId(record.getOperatorUserId())
                .operatorName(operator == null ? null : operator.getRealName())
                .fromStatus(record.getFromStatus() == null ? null : record.getFromStatus().getCode())
                .toStatus(record.getToStatus().getCode())
                .comment(record.getComment())
                .createTime(record.getCreateTime())
                .build();
    }
}
