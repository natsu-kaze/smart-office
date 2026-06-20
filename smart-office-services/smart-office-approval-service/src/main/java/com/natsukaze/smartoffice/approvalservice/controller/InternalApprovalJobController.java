package com.natsukaze.smartoffice.approvalservice.controller;

import com.natsukaze.smartoffice.approvalservice.service.ApprovalService;
import com.natsukaze.smartoffice.common.core.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/approvals/jobs")
@RequiredArgsConstructor
public class InternalApprovalJobController {

    private final ApprovalService approvalService;

    @PostMapping("/timeout-scan")
    public Result<Integer> scanTimeout(@RequestParam(defaultValue = "24") int timeoutHours) {
        return Result.success(approvalService.scanTimeoutApprovals(timeoutHours));
    }
}
