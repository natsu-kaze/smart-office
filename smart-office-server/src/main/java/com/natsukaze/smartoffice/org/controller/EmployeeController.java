package com.natsukaze.smartoffice.org.controller;

import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.org.dto.EmployeePageQuery;
import com.natsukaze.smartoffice.org.dto.EmployeeSaveRequest;
import com.natsukaze.smartoffice.org.service.OrgService;
import com.natsukaze.smartoffice.org.vo.EmployeeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/org/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final OrgService orgService;

    @GetMapping
    public Result<PageResult<EmployeeVO>> page(@ModelAttribute EmployeePageQuery query) {
        return Result.success(orgService.pageEmployees(query));
    }

    @PostMapping
    public Result<EmployeeVO> create(@Valid @RequestBody EmployeeSaveRequest request) {
        return Result.success(orgService.createEmployee(request));
    }

    @PutMapping("/{id}")
    public Result<EmployeeVO> update(@PathVariable Long id, @Valid @RequestBody EmployeeSaveRequest request) {
        return Result.success(orgService.updateEmployee(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        orgService.deleteEmployee(id);
        return Result.success();
    }
}
