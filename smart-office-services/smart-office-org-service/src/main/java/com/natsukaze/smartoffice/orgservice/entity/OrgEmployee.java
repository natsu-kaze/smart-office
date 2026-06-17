package com.natsukaze.smartoffice.orgservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("org_employee")
public class OrgEmployee extends BaseEntity {

    private Long userId;

    private String employeeNo;

    private Long departmentId;

    private Long positionId;

    private LocalDate hireDate;

    private String employmentStatus;
}
