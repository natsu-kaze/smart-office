package com.natsukaze.smartoffice.approvalservice.controller;

import com.natsukaze.smartoffice.approvalservice.dto.ApprovalActionRequest;
import com.natsukaze.smartoffice.approvalservice.dto.ApprovalFormRequest;
import com.natsukaze.smartoffice.approvalservice.dto.ApprovalPageQuery;
import com.natsukaze.smartoffice.approvalservice.dto.ApprovalRuleRequest;
import com.natsukaze.smartoffice.approvalservice.service.ApprovalService;
import com.natsukaze.smartoffice.approvalservice.vo.ApprovalFormVO;
import com.natsukaze.smartoffice.approvalservice.vo.ApprovalRecordVO;
import com.natsukaze.smartoffice.approvalservice.vo.ApprovalRuleVO;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final ApprovalService approvalService;

    @PostMapping
    public Result<ApprovalFormVO> createDraft(@RequestHeader(USER_ID_HEADER) Long userId,
                                              @Valid @RequestBody ApprovalFormRequest request) {
        return Result.success(approvalService.createDraft(userId, request));
    }

    @PutMapping("/{id}")
    public Result<ApprovalFormVO> updateDraft(@RequestHeader(USER_ID_HEADER) Long userId,
                                              @PathVariable Long id,
                                              @Valid @RequestBody ApprovalFormRequest request) {
        return Result.success(approvalService.updateDraft(userId, id, request));
    }

    @PostMapping("/{id}/submit")
    public Result<ApprovalFormVO> submit(@RequestHeader(USER_ID_HEADER) Long userId, @PathVariable Long id) {
        return Result.success(approvalService.submit(userId, id));
    }

    @PostMapping("/{id}/approve")
    public Result<ApprovalFormVO> approve(@RequestHeader(USER_ID_HEADER) Long userId,
                                          @PathVariable Long id,
                                          @RequestBody(required = false) ApprovalActionRequest request) {
        return Result.success(approvalService.approve(userId, id, request == null ? new ApprovalActionRequest() : request));
    }

    @PostMapping("/{id}/reject")
    public Result<ApprovalFormVO> reject(@RequestHeader(USER_ID_HEADER) Long userId,
                                         @PathVariable Long id,
                                         @RequestBody(required = false) ApprovalActionRequest request) {
        return Result.success(approvalService.reject(userId, id, request == null ? new ApprovalActionRequest() : request));
    }

    @PostMapping("/{id}/withdraw")
    public Result<ApprovalFormVO> withdraw(@RequestHeader(USER_ID_HEADER) Long userId,
                                           @PathVariable Long id,
                                           @RequestBody(required = false) ApprovalActionRequest request) {
        return Result.success(approvalService.withdraw(userId, id, request == null ? new ApprovalActionRequest() : request));
    }

    @PostMapping("/{id}/close")
    public Result<ApprovalFormVO> close(@RequestHeader(USER_ID_HEADER) Long userId,
                                        @PathVariable Long id,
                                        @RequestBody(required = false) ApprovalActionRequest request) {
        return Result.success(approvalService.close(userId, id, request == null ? new ApprovalActionRequest() : request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteDraft(@RequestHeader(USER_ID_HEADER) Long userId,
                                     @PathVariable Long id) {
        approvalService.deleteDraft(userId, id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<ApprovalFormVO> detail(@PathVariable Long id) {
        return Result.success(approvalService.detail(id));
    }

    @GetMapping("/my")
    public Result<PageResult<ApprovalFormVO>> myApplications(@RequestHeader(USER_ID_HEADER) Long userId,
                                                             @ModelAttribute ApprovalPageQuery query) {
        return Result.success(approvalService.myApplications(userId, query));
    }

    @GetMapping("/todos")
    public Result<PageResult<ApprovalFormVO>> myTodos(@RequestHeader(USER_ID_HEADER) Long userId,
                                                      @ModelAttribute ApprovalPageQuery query) {
        return Result.success(approvalService.myTodos(userId, query));
    }

    @GetMapping("/{id}/records")
    public Result<List<ApprovalRecordVO>> records(@PathVariable Long id) {
        return Result.success(approvalService.records(id));
    }

    @GetMapping("/{id}/timeline")
    public Result<List<ApprovalRecordVO>> timeline(@PathVariable Long id) {
        return Result.success(approvalService.records(id));
    }

    @GetMapping("/rules")
    public Result<List<ApprovalRuleVO>> rules() {
        return Result.success(approvalService.listRules());
    }

    @PostMapping("/rules")
    public Result<ApprovalRuleVO> createRule(@Valid @RequestBody ApprovalRuleRequest request) {
        return Result.success(approvalService.saveRule(request));
    }

    @PutMapping("/rules/{id}")
    public Result<ApprovalRuleVO> updateRule(@PathVariable Long id, @Valid @RequestBody ApprovalRuleRequest request) {
        return Result.success(approvalService.updateRule(id, request));
    }

    @DeleteMapping("/rules/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        approvalService.deleteRule(id);
        return Result.success();
    }
}
