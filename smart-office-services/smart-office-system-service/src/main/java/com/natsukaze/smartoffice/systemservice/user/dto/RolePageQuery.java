package com.natsukaze.smartoffice.systemservice.user.dto;

import lombok.Data;

@Data
public class RolePageQuery {

    private long current = 1;

    private long size = 10;

    private String keyword;

    private Integer status;
}
