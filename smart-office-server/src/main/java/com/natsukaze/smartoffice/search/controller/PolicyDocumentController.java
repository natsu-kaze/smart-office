package com.natsukaze.smartoffice.search.controller;

import com.natsukaze.smartoffice.auth.security.UserPrincipal;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.search.dto.PolicyDocumentPageQuery;
import com.natsukaze.smartoffice.search.dto.PolicyDocumentSaveRequest;
import com.natsukaze.smartoffice.search.service.PolicyDocumentService;
import com.natsukaze.smartoffice.search.vo.PolicyDocumentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyDocumentController {

    private final PolicyDocumentService documentService;

    @GetMapping
    public Result<PageResult<PolicyDocumentVO>> page(@ModelAttribute PolicyDocumentPageQuery query) {
        return Result.success(documentService.page(query));
    }

    @PostMapping
    public Result<PolicyDocumentVO> create(@AuthenticationPrincipal UserPrincipal principal,
                                           @Valid @RequestBody PolicyDocumentSaveRequest request) {
        return Result.success(documentService.create(principal.getUserId(), request));
    }

    @PutMapping("/{id}")
    public Result<PolicyDocumentVO> update(@PathVariable Long id, @Valid @RequestBody PolicyDocumentSaveRequest request) {
        return Result.success(documentService.update(id, request));
    }

    @GetMapping("/{id}")
    public Result<PolicyDocumentVO> detail(@PathVariable Long id) {
        return Result.success(documentService.detail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.success();
    }
}
