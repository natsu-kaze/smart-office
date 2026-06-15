package com.natsukaze.smartoffice.ai.controller;

import com.natsukaze.smartoffice.ai.dto.AiApprovalDraftRequest;
import com.natsukaze.smartoffice.ai.dto.AiChatRequest;
import com.natsukaze.smartoffice.ai.service.AiAssistantService;
import com.natsukaze.smartoffice.ai.vo.AiApprovalDraftVO;
import com.natsukaze.smartoffice.ai.vo.AiChatResponse;
import com.natsukaze.smartoffice.auth.security.UserPrincipal;
import com.natsukaze.smartoffice.common.core.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@AuthenticationPrincipal UserPrincipal principal,
                                       @Valid @RequestBody AiChatRequest request) {
        return Result.success(aiAssistantService.chat(principal.getUserId(), request));
    }

    @PostMapping("/approval-draft")
    public Result<AiApprovalDraftVO> draftApproval(@Valid @RequestBody AiApprovalDraftRequest request) {
        return Result.success(aiAssistantService.draftApproval(request));
    }

    @PostMapping("/approval-summary")
    public Result<String> summarizeApproval(@RequestBody Map<String, String> request) {
        return Result.success(aiAssistantService.summarizeApproval(request.get("content")));
    }
}
