package com.natsukaze.smartoffice.org.vo;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class DepartmentTreeVO {

    private Long id;

    private Long parentId;

    private String departmentCode;

    private String departmentName;

    private Long leaderUserId;

    private String leaderName;

    private Integer sort;

    private Integer status;

    @Builder.Default
    private List<DepartmentTreeVO> children = new ArrayList<>();
}
