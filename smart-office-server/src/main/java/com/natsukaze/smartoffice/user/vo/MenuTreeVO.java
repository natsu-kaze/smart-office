package com.natsukaze.smartoffice.user.vo;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class MenuTreeVO {

    private Long id;

    private Long parentId;

    private String menuName;

    private String menuType;

    private String path;

    private String component;

    private String permission;

    private String icon;

    private Integer sort;

    private Integer visible;

    private Integer status;

    @Builder.Default
    private List<MenuTreeVO> children = new ArrayList<>();
}
