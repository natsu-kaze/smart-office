package com.natsukaze.smartoffice.orgservice.controller;

import com.natsukaze.smartoffice.api.org.dto.OrgEmployeeDTO;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.orgservice.service.OrgService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/org/employees")
@RequiredArgsConstructor
public class InternalOrgEmployeeController {

    private final OrgService orgService;

    @GetMapping("/user/{userId}")
    public Result<OrgEmployeeDTO> getByUserId(@PathVariable Long userId) {
        return Result.success(orgService.getEmployeeByUserId(userId));
    }
}
