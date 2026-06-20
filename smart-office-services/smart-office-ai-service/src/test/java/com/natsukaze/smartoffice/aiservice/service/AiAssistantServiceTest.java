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
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.common.enums.AiMessageRole;
import com.natsukaze.smartoffice.common.enums.ApprovalType;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAssistantServiceTest {

    private static final long USER_ID = 3L;

    @Mock
    private AiConversationMapper conversationMapper;

    @Mock
    private AiMessageMapper messageMapper;

    @Mock
    private PolicySearchClient policySearchClient;

    @InjectMocks
    private AiAssistantService aiAssistantService;

    @Test
    void chatCreatesConversationAndStoresUserAndAssistantMessages() {
        doAnswer(invocation -> {
            AiConversation conversation = invocation.getArgument(0);
            conversation.setId(100L);
            return 1;
        }).when(conversationMapper).insert(any(AiConversation.class));
        AiChatRequest request = new AiChatRequest();
        request.setMessage("帮我总结一下报销流程");

        AiChatResponse response = aiAssistantService.chat(USER_ID, request);

        assertThat(response.getConversationId()).isEqualTo(100L);
        assertThat(response.getDegraded()).isTrue();
        assertThat(response.getAnswer()).contains("AI 模型暂未配置").contains(request.getMessage());

        ArgumentCaptor<AiMessage> messageCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues())
                .extracting(AiMessage::getRole)
                .containsExactly(AiMessageRole.USER, AiMessageRole.ASSISTANT);
    }

    @Test
    void chatRejectsConversationOwnedByAnotherUser() {
        AiConversation conversation = new AiConversation();
        conversation.setId(200L);
        conversation.setUserId(99L);
        when(conversationMapper.selectById(200L)).thenReturn(conversation);
        AiChatRequest request = new AiChatRequest();
        request.setConversationId(200L);
        request.setMessage("继续上次的话题");

        assertThatThrownBy(() -> aiAssistantService.chat(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("conversation does not belong to current user");
    }

    @Test
    void policyQuestionUsesSearchResultAsReferenceContext() {
        PolicyDocumentDTO document = new PolicyDocumentDTO();
        document.setTitle("报销管理制度");
        document.setDocumentVersion("v1.0");
        document.setContent("报销金额超过 1000 元时，需要部门负责人审批后继续流转到财务审批。");
        when(policySearchClient.search("高额报销怎么审批", "PUBLISHED", 1, 3))
                .thenReturn(Result.success(new PageResult<>(List.of(document), 1L, 1L, 3L)));
        AiPolicyQuestionRequest request = new AiPolicyQuestionRequest();
        request.setQuestion("高额报销怎么审批");

        AiPolicyAnswerVO answer = aiAssistantService.answerPolicyQuestion(request);

        assertThat(answer.getDegraded()).isTrue();
        assertThat(answer.getAnswer()).contains("报销管理制度").contains("财务审批");
        assertThat(answer.getReferences()).containsExactly("报销管理制度 v1.0");
    }

    @Test
    void policyQuestionFallsBackWhenSearchIsUnavailable() {
        when(policySearchClient.search("制度在哪里", "PUBLISHED", 1, 3))
                .thenReturn(new Result<>(500, "search unavailable", null));
        AiPolicyQuestionRequest request = new AiPolicyQuestionRequest();
        request.setQuestion("制度在哪里");

        AiPolicyAnswerVO answer = aiAssistantService.answerPolicyQuestion(request);

        assertThat(answer.getDegraded()).isTrue();
        assertThat(answer.getAnswer()).contains("制度检索暂不可用");
        assertThat(answer.getReferences()).isEmpty();
    }

    @Test
    void approvalDraftAndRiskUseOfficeRulesWhenModelIsUnavailable() {
        AiApprovalDraftRequest draftRequest = new AiApprovalDraftRequest();
        draftRequest.setText("我要报销 1280 元差旅费用");

        AiApprovalDraftVO draft = aiAssistantService.draftApproval(draftRequest);

        assertThat(draft.getApprovalType()).isEqualTo(ApprovalType.EXPENSE.getCode());
        assertThat(draft.getTitle()).startsWith("AI草稿-");

        AiApprovalRiskRequest riskRequest = new AiApprovalRiskRequest();
        riskRequest.setApprovalType(ApprovalType.EXPENSE.getCode());
        riskRequest.setAmount(BigDecimal.valueOf(1280));
        riskRequest.setContent("差旅报销");

        AiApprovalRiskVO risk = aiAssistantService.assessApprovalRisk(riskRequest);

        assertThat(risk.getDegraded()).isTrue();
        assertThat(risk.getLevel()).isEqualTo("MEDIUM");
        assertThat(risk.getWarnings()).anySatisfy(warning -> assertThat(warning).contains("财务审批"));
    }
}
