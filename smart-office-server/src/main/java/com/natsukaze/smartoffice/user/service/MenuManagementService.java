package com.natsukaze.smartoffice.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.natsukaze.smartoffice.user.entity.SysMenu;
import com.natsukaze.smartoffice.user.mapper.SysMenuMapper;
import com.natsukaze.smartoffice.user.vo.MenuTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuManagementService {

    private final SysMenuMapper sysMenuMapper;

    public List<MenuTreeVO> tree() {
        List<MenuTreeVO> nodes = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                        .orderByAsc(SysMenu::getSort)
                        .orderByAsc(SysMenu::getId))
                .stream()
                .map(this::toTreeVO)
                .toList();
        Map<Long, MenuTreeVO> nodeMap = nodes.stream()
                .collect(Collectors.toMap(MenuTreeVO::getId, node -> node));
        nodes.forEach(node -> {
            if (node.getParentId() != null && node.getParentId() != 0) {
                MenuTreeVO parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        });
        return nodes.stream()
                .filter(node -> node.getParentId() == null || node.getParentId() == 0)
                .sorted(Comparator.comparing(MenuTreeVO::getSort, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private MenuTreeVO toTreeVO(SysMenu menu) {
        return MenuTreeVO.builder()
                .id(menu.getId())
                .parentId(menu.getParentId())
                .menuName(menu.getMenuName())
                .menuType(menu.getMenuType())
                .path(menu.getPath())
                .component(menu.getComponent())
                .permission(menu.getPermission())
                .icon(menu.getIcon())
                .sort(menu.getSort())
                .visible(menu.getVisible())
                .status(menu.getStatus())
                .build();
    }
}
