package com.natsukaze.smartoffice.org.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class EmployeeVO {

    private Long id;

    private Long userId;

    private String username;

    private String realName;

    private String employeeNo;

    private Long departmentId;

    private String departmentName;

    private Long positionId;

    private String positionName;

    private LocalDate hireDate;

    private String employmentStatus;
}
