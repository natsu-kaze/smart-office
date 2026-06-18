package com.natsukaze.smartoffice.searchservice.controller;

import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.searchservice.service.PolicyDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/search/policies")
@RequiredArgsConstructor
public class InternalPolicyDocumentController {

    private final PolicyDocumentService documentService;

    @PostMapping("/reindex")
    public Result<Integer> reindex() {
        return Result.success(documentService.reindex());
    }
}
