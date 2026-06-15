package com.natsukaze.smartoffice.user.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoleVO {

    private Long id;

    private String roleCode;

    private String roleName;

    private Integer sort;

    private Integer status;

    private String remark;

    private List<Long> menuIds;
}
