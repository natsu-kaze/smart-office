package com.natsukaze.smartoffice.aiservice.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiApprovalRiskVO {

    private String level;

    private List<String> warnings;

    private String suggestion;

    private Boolean degraded;
}
