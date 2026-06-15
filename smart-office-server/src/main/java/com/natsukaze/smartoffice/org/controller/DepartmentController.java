package com.natsukaze.smartoffice.org.controller;

import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.org.dto.DepartmentLeaderRequest;
import com.natsukaze.smartoffice.org.dto.DepartmentSaveRequest;
import com.natsukaze.smartoffice.org.service.OrgService;
import com.natsukaze.smartoffice.org.vo.DepartmentTreeVO;
import com.natsukaze.smartoffice.org.vo.EmployeeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/org/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final OrgService orgService;

    @GetMapping("/tree")
    public Result<List<DepartmentTreeVO>> tree() {
        return Result.success(orgService.departmentTree());
    }

    @PostMapping
    public Result<DepartmentTreeVO> create(@Valid @RequestBody DepartmentSaveRequest request) {
        return Result.success(orgService.createDepartment(request));
    }

    @PutMapping("/{id}")
    public Result<DepartmentTreeVO> update(@PathVariable Long id, @Valid @RequestBody DepartmentSaveRequest request) {
        return Result.success(orgService.updateDepartment(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        orgService.deleteDepartment(id);
        return Result.success();
    }

    @PutMapping("/{id}/leader")
    public Result<Void> setLeader(@PathVariable Long id, @Valid @RequestBody DepartmentLeaderRequest request) {
        orgService.setDepartmentLeader(id, request);
        return Result.success();
    }

    @GetMapping("/{id}/employees")
    public Result<List<EmployeeVO>> employees(@PathVariable Long id) {
        return Result.success(orgService.departmentEmployees(id));
    }
}
