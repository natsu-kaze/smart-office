package com.natsukaze.smartoffice.orgservice.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyVO {

    private Long id;

    private String companyCode;

    private String companyName;

    private String contactName;

    private String contactPhone;

    private String address;

    private Integer status;
}
