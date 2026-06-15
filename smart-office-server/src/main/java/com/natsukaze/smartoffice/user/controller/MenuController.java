package com.natsukaze.smartoffice.user.controller;

import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.user.service.MenuManagementService;
import com.natsukaze.smartoffice.user.vo.MenuTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuManagementService menuManagementService;

    @GetMapping("/tree")
    public Result<List<MenuTreeVO>> tree() {
        return Result.success(menuManagementService.tree());
    }
}
