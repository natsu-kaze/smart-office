package com.natsukaze.smartoffice.org.controller;

import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.org.dto.PositionPageQuery;
import com.natsukaze.smartoffice.org.dto.PositionSaveRequest;
import com.natsukaze.smartoffice.org.service.OrgService;
import com.natsukaze.smartoffice.org.vo.PositionVO;
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
@RequestMapping("/api/org/positions")
@RequiredArgsConstructor
public class PositionController {

    private final OrgService orgService;

    @GetMapping
    public Result<PageResult<PositionVO>> page(@ModelAttribute PositionPageQuery query) {
        return Result.success(orgService.pagePositions(query));
    }

    @PostMapping
    public Result<PositionVO> create(@Valid @RequestBody PositionSaveRequest request) {
        return Result.success(orgService.createPosition(request));
    }

    @PutMapping("/{id}")
    public Result<PositionVO> update(@PathVariable Long id, @Valid @RequestBody PositionSaveRequest request) {
        return Result.success(orgService.updatePosition(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        orgService.deletePosition(id);
        return Result.success();
    }
}
