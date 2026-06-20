package com.natsukaze.smartoffice.searchservice.controller;

import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.searchservice.dto.PolicyDocumentPageQuery;
import com.natsukaze.smartoffice.searchservice.dto.PolicyDocumentSaveRequest;
import com.natsukaze.smartoffice.searchservice.dto.PolicyDocumentUploadRequest;
import com.natsukaze.smartoffice.searchservice.service.PolicyDocumentService;
import com.natsukaze.smartoffice.searchservice.vo.PolicyDocumentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

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
    public Result<PolicyDocumentVO> create(@RequestHeader("X-User-Id") Long userId,
                                           @RequestHeader("X-Username") String username,
                                           @Valid @RequestBody PolicyDocumentSaveRequest request) {
        return Result.success(documentService.create(userId, username, request));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<PolicyDocumentVO> upload(@RequestHeader("X-User-Id") Long userId,
                                           @RequestHeader("X-Username") String username,
                                           @ModelAttribute PolicyDocumentUploadRequest request,
                                           @RequestParam("file") MultipartFile file) {
        return Result.success(documentService.upload(userId, username, request, file));
    }

    @PutMapping("/{id}")
    public Result<PolicyDocumentVO> update(@RequestHeader("X-Username") String username,
                                           @PathVariable Long id,
                                           @Valid @RequestBody PolicyDocumentSaveRequest request) {
        return Result.success(documentService.update(username, id, request));
    }

    @GetMapping("/{id}")
    public Result<PolicyDocumentVO> detail(@PathVariable Long id) {
        return Result.success(documentService.detail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader("X-Username") String username, @PathVariable Long id) {
        documentService.delete(username, id);
        return Result.success();
    }
}
