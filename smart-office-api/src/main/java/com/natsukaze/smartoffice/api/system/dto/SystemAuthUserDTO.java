package com.natsukaze.smartoffice.api.system.dto;

import java.util.List;

public record SystemAuthUserDTO(
        Long userId,
        String username,
        String password,
        String realName,
        String phone,
        String email,
        String avatar,
        Integer status,
        List<String> roles,
        List<String> permissions
) {
}
