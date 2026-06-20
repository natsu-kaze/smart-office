package com.natsukaze.smartoffice.aiservice.controller;

import com.natsukaze.smartoffice.aiservice.dto.AiApprovalDraftRequest;
import com.natsukaze.smartoffice.aiservice.dto.AiApprovalRiskRequest;
import com.natsukaze.smartoffice.aiservice.dto.AiChatRequest;
import com.natsukaze.smartoffice.aiservice.dto.AiPolicyQuestionRequest;
import com.natsukaze.smartoffice.aiservice.dto.AiSummaryRequest;
import com.natsukaze.smartoffice.aiservice.service.AiAssistantService;
import com.natsukaze.smartoffice.aiservice.vo.AiApprovalDraftVO;
import com.natsukaze.smartoffice.aiservice.vo.AiApprovalRiskVO;
import com.natsukaze.smartoffice.aiservice.vo.AiChatResponse;
import com.natsukaze.smartoffice.aiservice.vo.AiPolicyAnswerVO;
import com.natsukaze.smartoffice.common.core.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final AiAssistantService aiAssistantService;

    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@RequestHeader(USER_ID_HEADER) Long userId,
                                       @Valid @RequestBody AiChatRequest request) {
        return Result.success(aiAssistantService.chat(userId, request));
    }

    @PostMapping("/policy-qa")
    public Result<AiPolicyAnswerVO> answerPolicyQuestion(@Valid @RequestBody AiPolicyQuestionRequest request) {
        return Result.success(aiAssistantService.answerPolicyQuestion(request));
    }

    @PostMapping("/approval-draft")
    public Result<AiApprovalDraftVO> draftApproval(@Valid @RequestBody AiApprovalDraftRequest request) {
        return Result.success(aiAssistantService.draftApproval(request));
    }

    @PostMapping("/approval-summary")
    public Result<String> summarizeApproval(@Valid @RequestBody AiSummaryRequest request) {
        return Result.success(aiAssistantService.summarizeApproval(request.getContent()));
    }

    @PostMapping("/approval-risk")
    public Result<AiApprovalRiskVO> assessApprovalRisk(@RequestBody AiApprovalRiskRequest request) {
        return Result.success(aiAssistantService.assessApprovalRisk(request));
    }
}
