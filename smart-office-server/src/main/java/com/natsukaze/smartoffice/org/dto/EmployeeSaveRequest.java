package com.natsukaze.smartoffice.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeSaveRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String employeeNo;

    @NotNull
    private Long departmentId;

    private Long positionId;

    private LocalDate hireDate;

    private String employmentStatus;
}
