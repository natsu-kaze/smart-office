package com.natsukaze.smartoffice.orgservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DepartmentLeaderRequest {

    @NotNull
    private Long leaderUserId;
}
