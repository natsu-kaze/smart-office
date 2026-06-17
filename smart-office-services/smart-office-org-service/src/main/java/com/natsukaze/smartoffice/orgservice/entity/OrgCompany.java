package com.natsukaze.smartoffice.orgservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.natsukaze.smartoffice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("org_company")
public class OrgCompany extends BaseEntity {

    private String companyCode;

    private String companyName;

    private String contactName;

    private String contactPhone;

    private String address;

    private Integer status;
}
