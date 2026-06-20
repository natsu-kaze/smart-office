package com.natsukaze.smartoffice.aiservice.service;

import com.natsukaze.smartoffice.aiservice.dto.AiApprovalDraftRequest;
import com.natsukaze.smartoffice.aiservice.dto.AiApprovalRiskRequest;
import com.natsukaze.smartoffice.aiservice.dto.AiChatRequest;
import com.natsukaze.smartoffice.aiservice.dto.AiPolicyQuestionRequest;
import com.natsukaze.smartoffice.aiservice.entity.AiConversation;
import com.natsukaze.smartoffice.aiservice.entity.AiMessage;
import com.natsukaze.smartoffice.aiservice.mapper.AiConversationMapper;
import com.natsukaze.smartoffice.aiservice.mapper.AiMessageMapper;
import com.natsukaze.smartoffice.aiservice.vo.AiApprovalDraftVO;
import com.natsukaze.smartoffice.aiservice.vo.AiApprovalRiskVO;
import com.natsukaze.smartoffice.aiservice.vo.AiChatResponse;
import com.natsukaze.smartoffice.aiservice.vo.AiPolicyAnswerVO;
import com.natsukaze.smartoffice.api.search.client.PolicySearchClient;
import com.natsukaze.smartoffice.api.search.dto.PolicyDocumentDTO;
import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.common.enums.AiMessageRole;
import com.natsukaze.smartoffice.common.enums.ApprovalType;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiAssistantService {

    private static final BigDecimal HIGH_EXPENSE_AMOUNT = BigDecimal.valueOf(1000);

    private final AiConversationMapper conversationMapper;

    private final AiMessageMapper messageMapper;

    private final PolicySearchClient policySearchClient;

    @Transactional
    public AiChatResponse chat(Long userId, AiChatRequest request) {
        AiConversation conversation = ensureConversation(userId, request.getConversationId(), request.getMessage());
        saveMessage(conversation.getId(), AiMessageRole.USER, request.getMessage());
        String answer = buildFallbackChatAnswer(request.getMessage());
        saveMessage(conversation.getId(), AiMessageRole.ASSISTANT, answer);
        return AiChatResponse.builder()
                .conversationId(conversation.getId())
                .answer(answer)
                .degraded(true)
                .build();
    }

    public AiPolicyAnswerVO answerPolicyQuestion(AiPolicyQuestionRequest request) {
        Result<PageResult<PolicyDocumentDTO>> result;
        try {
            result = policySearchClient.search(request.getQuestion(), "PUBLISHED", 1, 3);
        } catch (RuntimeException ex) {
            log.warn("policy search unavailable, fallback to degraded answer, question={}", request.getQuestion(), ex);
            return policySearchUnavailable();
        }
        if (result == null || result.code() != ErrorCode.SUCCESS.getCode() || result.data() == null) {
            return policySearchUnavailable();
        }
        List<PolicyDocumentDTO> documents = result.data().getRecords();
        if (documents == null || documents.isEmpty()) {
            return AiPolicyAnswerVO.builder()
                    .answer("没有检索到相关制度。你可以换一个关键词，或先在制度文档中补充对应内容。")
                    .references(List.of())
                    .degraded(true)
                    .build();
        }
        List<String> references = documents.stream()
                .map(document -> document.getTitle() + " " + nullToEmpty(document.getDocumentVersion()))
                .toList();
        PolicyDocumentDTO first = documents.get(0);
        return AiPolicyAnswerVO.builder()
                .answer("根据已发布制度《" + first.getTitle() + "》：" + summarize(first.getContent(), 180))
                .references(references)
                .degraded(true)
                .build();
    }

    private AiPolicyAnswerVO policySearchUnavailable() {
        return AiPolicyAnswerVO.builder()
                .answer("制度检索暂不可用，请稍后重试或直接使用制度文档检索。")
                .references(List.of())
                .degraded(true)
                .build();
    }

    public AiApprovalDraftVO draftApproval(AiApprovalDraftRequest request) {
        String text = request.getText();
        ApprovalType type = detectApprovalType(text);
        return AiApprovalDraftVO.builder()
                .approvalType(type.getCode())
                .title("AI草稿-" + type.getDesc())
                .content("{\"source\":\"" + escapeJson(text) + "\"}")
                .suggestion("已按规则生成草稿，提交前请补充时间、金额、原因等结构化字段。")
                .build();
    }

    public String summarizeApproval(String content) {
        if (!StringUtils.hasText(content)) {
            return "审批内容为空，无法生成摘要。";
        }
        return "审批摘要：" + summarize(content, 120);
    }

    public AiApprovalRiskVO assessApprovalRisk(AiApprovalRiskRequest request) {
        List<String> warnings = new ArrayList<>();
        ApprovalType approvalType = StringUtils.hasText(request.getApprovalType())
                ? ApprovalType.of(request.getApprovalType())
                : null;
        if (approvalType == ApprovalType.EXPENSE
                && request.getAmount() != null
                && request.getAmount().compareTo(HIGH_EXPENSE_AMOUNT) > 0) {
            warnings.add("报销金额超过 1000 元，需要部门负责人审批后继续流转到财务审批。");
        }
        if (StringUtils.hasText(request.getContent()) && request.getContent().length() < 10) {
            warnings.add("审批说明较短，建议补充事由、时间、凭证或背景。");
        }
        String level = warnings.isEmpty() ? "LOW" : "MEDIUM";
        return AiApprovalRiskVO.builder()
                .level(level)
                .warnings(warnings)
                .suggestion(warnings.isEmpty() ? "未发现明显风险，可按普通审批流程处理。" : "请审批人结合制度和附件重点核对。")
                .degraded(true)
                .build();
    }

    private AiConversation ensureConversation(Long userId, Long conversationId, String firstMessage) {
        if (conversationId != null) {
            AiConversation existing = conversationMapper.selectById(conversationId);
            if (existing != null) {
                if (!existing.getUserId().equals(userId)) {
                    throw new BusinessException("conversation does not belong to current user");
                }
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

    private String buildFallbackChatAnswer(String message) {
        return "AI 模型暂未配置，已使用规则降级处理。你可以继续使用制度检索、审批摘要、智能填单和风险提示能力。原始问题：" + message;
    }

    private ApprovalType detectApprovalType(String text) {
        if (text.contains("报销") || text.contains("费用") || text.contains("发票")) {
            return ApprovalType.EXPENSE;
        }
        if (text.contains("加班")) {
            return ApprovalType.OVERTIME;
        }
        if (text.contains("请假") || text.contains("调休") || text.contains("病假") || text.contains("事假")) {
            return ApprovalType.LEAVE;
        }
        return ApprovalType.GENERAL;
    }

    private String summarize(String content, int limit) {
        if (!StringUtils.hasText(content)) {
            return "暂无可摘要内容。";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() > limit ? normalized.substring(0, limit) + "..." : normalized;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
