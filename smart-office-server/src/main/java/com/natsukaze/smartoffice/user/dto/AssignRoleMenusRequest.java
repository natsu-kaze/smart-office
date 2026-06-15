package com.natsukaze.smartoffice.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AssignRoleMenusRequest {

    @NotNull
    private List<Long> menuIds = new ArrayList<>();
}
