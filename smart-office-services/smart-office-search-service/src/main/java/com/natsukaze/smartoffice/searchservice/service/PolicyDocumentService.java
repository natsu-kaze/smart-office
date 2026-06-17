package com.natsukaze.smartoffice.searchservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.enums.DocumentStatus;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import com.natsukaze.smartoffice.searchservice.dto.PolicyDocumentPageQuery;
import com.natsukaze.smartoffice.searchservice.dto.PolicyDocumentSaveRequest;
import com.natsukaze.smartoffice.searchservice.entity.PolicyDocument;
import com.natsukaze.smartoffice.searchservice.mapper.PolicyDocumentMapper;
import com.natsukaze.smartoffice.searchservice.vo.PolicyDocumentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PolicyDocumentService {

    private final PolicyDocumentMapper documentMapper;

    public PageResult<PolicyDocumentVO> page(PolicyDocumentPageQuery query) {
        LambdaQueryWrapper<PolicyDocument> wrapper = new LambdaQueryWrapper<PolicyDocument>()
                .eq(StringUtils.hasText(query.getStatus()), PolicyDocument::getStatus,
                        DocumentStatus.ofNullable(query.getStatus()))
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(PolicyDocument::getTitle, query.getKeyword())
                        .or()
                        .like(PolicyDocument::getContent, query.getKeyword())
                        .or()
                        .like(PolicyDocument::getSummary, query.getKeyword()))
                .orderByDesc(PolicyDocument::getUpdateTime);
        Page<PolicyDocument> page = documentMapper.selectPage(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.from(page.convert(this::toVO));
    }

    @Transactional
    public PolicyDocumentVO create(Long publisherId, PolicyDocumentSaveRequest request) {
        PolicyDocument document = new PolicyDocument();
        fill(document, request);
        document.setPublisherId(publisherId);
        if (document.getStatus() == DocumentStatus.PUBLISHED) {
            document.setPublishedAt(LocalDateTime.now());
        }
        documentMapper.insert(document);
        return toVO(document);
    }

    @Transactional
    public PolicyDocumentVO update(Long id, PolicyDocumentSaveRequest request) {
        PolicyDocument document = requireDocument(id);
        fill(document, request);
        if (document.getStatus() == DocumentStatus.PUBLISHED && document.getPublishedAt() == null) {
            document.setPublishedAt(LocalDateTime.now());
        }
        documentMapper.updateById(document);
        return toVO(requireDocument(id));
    }

    public PolicyDocumentVO detail(Long id) {
        return toVO(requireDocument(id));
    }

    @Transactional
    public void delete(Long id) {
        requireDocument(id);
        documentMapper.deleteById(id);
    }

    private void fill(PolicyDocument document, PolicyDocumentSaveRequest request) {
        document.setTitle(request.getTitle());
        document.setContent(request.getContent());
        document.setSummary(request.getSummary());
        document.setDocumentVersion(request.getDocumentVersion());
        document.setStatus(StringUtils.hasText(request.getStatus()) ? DocumentStatus.ofNullable(request.getStatus()) : DocumentStatus.DRAFT);
    }

    private PolicyDocument requireDocument(Long id) {
        PolicyDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException("policy document not found");
        }
        return document;
    }

    private PolicyDocumentVO toVO(PolicyDocument document) {
        return PolicyDocumentVO.builder()
                .id(document.getId())
                .title(document.getTitle())
                .content(document.getContent())
                .summary(document.getSummary())
                .documentVersion(document.getDocumentVersion())
                .status(document.getStatus() == null ? null : document.getStatus().getCode())
                .publisherId(document.getPublisherId())
                .publishedAt(document.getPublishedAt())
                .build();
    }
}
