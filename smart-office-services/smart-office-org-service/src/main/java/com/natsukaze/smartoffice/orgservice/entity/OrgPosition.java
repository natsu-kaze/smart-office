package com.natsukaze.smartoffice.orgservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("org_position")
public class OrgPosition extends BaseEntity {

    private Long departmentId;

    private String positionCode;

    private String positionName;

    private Integer sort;

    private Integer status;
}
