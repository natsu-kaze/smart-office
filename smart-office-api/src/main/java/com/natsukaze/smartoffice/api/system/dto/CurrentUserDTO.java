package com.natsukaze.smartoffice.api.system.dto;

public record CurrentUserDTO(
        Long userId,
        String username,
        String realName,
        Long employeeId,
        Long departmentId
) {
}
