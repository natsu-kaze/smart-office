package com.natsukaze.smartoffice.searchservice.controller;

import com.natsukaze.smartoffice.api.search.dto.PolicyDocumentDTO;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.searchservice.dto.PolicyDocumentPageQuery;
import com.natsukaze.smartoffice.searchservice.service.PolicyDocumentService;
import com.natsukaze.smartoffice.searchservice.vo.PolicyDocumentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/search/policies")
@RequiredArgsConstructor
public class InternalPolicyDocumentController {

    private final PolicyDocumentService documentService;

    @GetMapping
    public Result<PageResult<PolicyDocumentDTO>> search(@ModelAttribute PolicyDocumentPageQuery query) {
        PageResult<PolicyDocumentVO> page = documentService.page(query);
        List<PolicyDocumentDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .toList();
        return Result.success(new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @PostMapping("/reindex")
    public Result<Integer> reindex() {
        return Result.success(documentService.reindex());
    }

    private PolicyDocumentDTO toDTO(PolicyDocumentVO source) {
        PolicyDocumentDTO dto = new PolicyDocumentDTO();
        dto.setId(source.getId());
        dto.setTitle(source.getTitle());
        dto.setContent(source.getContent());
        dto.setSummary(source.getSummary());
        dto.setDocumentVersion(source.getDocumentVersion());
        dto.setStatus(source.getStatus());
        dto.setPublisherId(source.getPublisherId());
        dto.setPublishedAt(source.getPublishedAt());
        return dto;
    }
}
