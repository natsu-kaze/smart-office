package com.natsukaze.smartoffice.orgservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("org_department")
public class OrgDepartment extends BaseEntity {

    private Long parentId;

    private String departmentCode;

    private String departmentName;

    private Long leaderUserId;

    private Integer sort;

    private Integer status;
}
