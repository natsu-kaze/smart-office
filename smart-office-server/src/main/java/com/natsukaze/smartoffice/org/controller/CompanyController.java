package com.natsukaze.smartoffice.org.controller;

import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.org.dto.CompanyUpdateRequest;
import com.natsukaze.smartoffice.org.service.OrgService;
import com.natsukaze.smartoffice.org.vo.CompanyVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/org/company")
@RequiredArgsConstructor
public class CompanyController {

    private final OrgService orgService;

    @GetMapping
    public Result<CompanyVO> getCompany() {
        return Result.success(orgService.getCompany());
    }

    @PutMapping("/{id}")
    public Result<CompanyVO> updateCompany(@PathVariable Long id, @Valid @RequestBody CompanyUpdateRequest request) {
        return Result.success(orgService.updateCompany(id, request));
    }
}
