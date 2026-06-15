package com.natsukaze.smartoffice.approval.controller;

import com.natsukaze.smartoffice.approval.dto.ApprovalActionRequest;
import com.natsukaze.smartoffice.approval.dto.ApprovalFormRequest;
import com.natsukaze.smartoffice.approval.dto.ApprovalPageQuery;
import com.natsukaze.smartoffice.approval.service.ApprovalService;
import com.natsukaze.smartoffice.approval.vo.ApprovalFormVO;
import com.natsukaze.smartoffice.approval.vo.ApprovalRecordVO;
import com.natsukaze.smartoffice.auth.security.UserPrincipal;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping
    public Result<ApprovalFormVO> createDraft(@AuthenticationPrincipal UserPrincipal principal,
                                              @Valid @RequestBody ApprovalFormRequest request) {
        return Result.success(approvalService.createDraft(principal.getUserId(), request));
    }

    @PutMapping("/{id}")
    public Result<ApprovalFormVO> updateDraft(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long id,
                                              @Valid @RequestBody ApprovalFormRequest request) {
        return Result.success(approvalService.updateDraft(principal.getUserId(), id, request));
    }

    @PostMapping("/{id}/submit")
    public Result<ApprovalFormVO> submit(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return Result.success(approvalService.submit(principal.getUserId(), id));
    }

    @PostMapping("/{id}/approve")
    public Result<ApprovalFormVO> approve(@AuthenticationPrincipal UserPrincipal principal,
                                          @PathVariable Long id,
                                          @RequestBody(required = false) ApprovalActionRequest request) {
        return Result.success(approvalService.approve(principal.getUserId(), id, request == null ? new ApprovalActionRequest() : request));
    }

    @PostMapping("/{id}/reject")
    public Result<ApprovalFormVO> reject(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable Long id,
                                         @RequestBody(required = false) ApprovalActionRequest request) {
        return Result.success(approvalService.reject(principal.getUserId(), id, request == null ? new ApprovalActionRequest() : request));
    }

    @PostMapping("/{id}/withdraw")
    public Result<ApprovalFormVO> withdraw(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long id,
                                           @RequestBody(required = false) ApprovalActionRequest request) {
        return Result.success(approvalService.withdraw(principal.getUserId(), id, request == null ? new ApprovalActionRequest() : request));
    }

    @PostMapping("/{id}/close")
    public Result<ApprovalFormVO> close(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long id,
                                        @RequestBody(required = false) ApprovalActionRequest request) {
        return Result.success(approvalService.close(principal.getUserId(), id, request == null ? new ApprovalActionRequest() : request));
    }

    @GetMapping("/{id}")
    public Result<ApprovalFormVO> detail(@PathVariable Long id) {
        return Result.success(approvalService.detail(id));
    }

    @GetMapping("/my")
    public Result<PageResult<ApprovalFormVO>> myApplications(@AuthenticationPrincipal UserPrincipal principal,
                                                             @ModelAttribute ApprovalPageQuery query) {
        return Result.success(approvalService.myApplications(principal.getUserId(), query));
    }

    @GetMapping("/todos")
    public Result<PageResult<ApprovalFormVO>> myTodos(@AuthenticationPrincipal UserPrincipal principal,
                                                      @ModelAttribute ApprovalPageQuery query) {
        return Result.success(approvalService.myTodos(principal.getUserId(), query));
    }

    @GetMapping("/{id}/records")
    public Result<List<ApprovalRecordVO>> records(@PathVariable Long id) {
        return Result.success(approvalService.records(id));
    }

    @GetMapping("/{id}/timeline")
    public Result<List<ApprovalRecordVO>> timeline(@PathVariable Long id) {
        return Result.success(approvalService.records(id));
    }
}
