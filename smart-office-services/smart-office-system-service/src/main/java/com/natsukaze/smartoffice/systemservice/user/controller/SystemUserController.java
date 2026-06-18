package com.natsukaze.smartoffice.systemservice.user.controller;

import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.systemservice.user.dto.UserPageQuery;
import com.natsukaze.smartoffice.systemservice.user.service.SystemUserService;
import com.natsukaze.smartoffice.systemservice.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class SystemUserController {

    private final SystemUserService systemUserService;

    @GetMapping
    public Result<PageResult<UserVO>> page(@ModelAttribute UserPageQuery query) {
        return Result.success(systemUserService.page(query));
    }
}
