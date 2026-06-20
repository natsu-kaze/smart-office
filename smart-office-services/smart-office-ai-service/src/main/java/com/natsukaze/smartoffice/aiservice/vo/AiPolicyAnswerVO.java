package com.natsukaze.smartoffice.aiservice.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiPolicyAnswerVO {

    private String answer;

    private List<String> references;

    private Boolean degraded;
}
