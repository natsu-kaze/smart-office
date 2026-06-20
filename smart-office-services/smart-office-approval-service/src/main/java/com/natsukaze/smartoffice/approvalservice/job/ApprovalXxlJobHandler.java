package com.natsukaze.smartoffice.approvalservice.job;

import com.natsukaze.smartoffice.approvalservice.service.ApprovalService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalXxlJobHandler {

    private final ApprovalService approvalService;

    @XxlJob("approvalTimeoutScanJob")
    public void timeoutScan() {
        int timeoutHours = parseTimeoutHours(XxlJobHelper.getJobParam());
        int count = approvalService.scanTimeoutApprovals(timeoutHours);
        String message = "Approval timeout scan finished. timeoutHours=" + timeoutHours + ", count=" + count;
        log.info(message);
        XxlJobHelper.handleSuccess(message);
    }

    private int parseTimeoutHours(String param) {
        if (!StringUtils.hasText(param)) {
            return 24;
        }
        try {
            return Integer.parseInt(param);
        } catch (NumberFormatException ex) {
            log.warn("Invalid approval timeout scan param: {}", param);
            return 24;
        }
    }
}
