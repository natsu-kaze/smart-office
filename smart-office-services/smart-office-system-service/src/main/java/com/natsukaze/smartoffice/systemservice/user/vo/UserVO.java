package com.natsukaze.smartoffice.systemservice.user.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserVO {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private String email;

    private String avatar;

    private Integer status;

    private LocalDateTime lastLoginTime;

    private List<Long> roleIds;

    private List<String> roleCodes;

    private List<String> roleNames;
}
