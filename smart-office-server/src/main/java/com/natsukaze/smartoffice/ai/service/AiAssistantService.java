package com.natsukaze.smartoffice.ai.service;

import com.natsukaze.smartoffice.ai.dto.AiApprovalDraftRequest;
import com.natsukaze.smartoffice.ai.dto.AiChatRequest;
import com.natsukaze.smartoffice.ai.entity.AiConversation;
import com.natsukaze.smartoffice.ai.entity.AiMessage;
import com.natsukaze.smartoffice.ai.mapper.AiConversationMapper;
import com.natsukaze.smartoffice.ai.mapper.AiMessageMapper;
import com.natsukaze.smartoffice.ai.vo.AiApprovalDraftVO;
import com.natsukaze.smartoffice.ai.vo.AiChatResponse;
import com.natsukaze.smartoffice.common.enums.AiMessageRole;
import com.natsukaze.smartoffice.common.enums.ApprovalType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final AiConversationMapper conversationMapper;

    private final AiMessageMapper messageMapper;

    @Transactional
    public AiChatResponse chat(Long userId, AiChatRequest request) {
        AiConversation conversation = ensureConversation(userId, request.getConversationId(), request.getMessage());
        saveMessage(conversation.getId(), AiMessageRole.USER, request.getMessage());
        String answer = "AI service is not configured yet. Fallback search/suggestion: " + request.getMessage();
        saveMessage(conversation.getId(), AiMessageRole.ASSISTANT, answer);
        return AiChatResponse.builder()
                .conversationId(conversation.getId())
                .answer(answer)
                .degraded(true)
                .build();
    }

    public AiApprovalDraftVO draftApproval(AiApprovalDraftRequest request) {
        String text = request.getText();
        ApprovalType type = text.contains("报销") ? ApprovalType.EXPENSE : (text.contains("加班") ? ApprovalType.OVERTIME : ApprovalType.LEAVE);
        return AiApprovalDraftVO.builder()
                .approvalType(type.getCode())
                .title("AI Draft - " + type.getCode())
                .content("{\"source\":\"" + escapeJson(text) + "\"}")
                .suggestion("Fallback draft generated without external model. Please review time, amount and reason before submitting.")
                .build();
    }

    public String summarizeApproval(String content) {
        if (content == null || content.isBlank()) {
            return "No approval content to summarize.";
        }
        String summary = content.length() > 120 ? content.substring(0, 120) + "..." : content;
        return "Fallback summary: " + summary;
    }

    private AiConversation ensureConversation(Long userId, Long conversationId, String firstMessage) {
        if (conversationId != null) {
            AiConversation existing = conversationMapper.selectById(conversationId);
            if (existing != null) {
                return existing;
            }
        }
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(firstMessage.length() > 30 ? firstMessage.substring(0, 30) : firstMessage);
        conversation.setStatus(1);
        conversationMapper.insert(conversation);
        return conversation;
    }

    private void saveMessage(Long conversationId, AiMessageRole role, String content) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        messageMapper.insert(message);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
