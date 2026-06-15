package com.natsukaze.smartoffice.org.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyUpdateRequest {

    @NotBlank
    private String companyName;

    private String contactName;

    private String contactPhone;

    private String address;

    private Integer status;
}
