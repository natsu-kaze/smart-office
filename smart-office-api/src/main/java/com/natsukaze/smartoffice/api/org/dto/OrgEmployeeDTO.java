package com.natsukaze.smartoffice.api.org.dto;

public record OrgEmployeeDTO(
        Long employeeId,
        Long userId,
        Long departmentId,
        String departmentName,
        Long departmentLeaderUserId
) {
}
