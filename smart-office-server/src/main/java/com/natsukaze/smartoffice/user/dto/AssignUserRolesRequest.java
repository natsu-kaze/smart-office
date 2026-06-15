package com.natsukaze.smartoffice.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AssignUserRolesRequest {

    @NotNull
    private List<Long> roleIds = new ArrayList<>();
}
