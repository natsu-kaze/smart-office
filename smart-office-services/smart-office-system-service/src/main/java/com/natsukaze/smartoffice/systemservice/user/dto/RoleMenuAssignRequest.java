package com.natsukaze.smartoffice.systemservice.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleMenuAssignRequest {

    private List<Long> menuIds;
}
