package com.natsukaze.smartoffice.searchservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.natsukaze.smartoffice.api.system.client.SystemUserClient;
import com.natsukaze.smartoffice.api.system.dto.SystemAuthUserDTO;
import com.natsukaze.smartoffice.common.core.ErrorCode;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.common.core.Result;
import com.natsukaze.smartoffice.common.enums.DocumentStatus;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import com.natsukaze.smartoffice.searchservice.dto.PolicyDocumentPageQuery;
import com.natsukaze.smartoffice.searchservice.dto.PolicyDocumentSaveRequest;
import com.natsukaze.smartoffice.searchservice.dto.PolicyDocumentUploadRequest;
import com.natsukaze.smartoffice.searchservice.entity.PolicyDocument;
import com.natsukaze.smartoffice.searchservice.es.PolicyDocumentIndexService;
import com.natsukaze.smartoffice.searchservice.mapper.PolicyDocumentMapper;
import com.natsukaze.smartoffice.searchservice.vo.PolicyDocumentVO;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PolicyDocumentService {

    private final PolicyDocumentMapper documentMapper;

    private final PolicyDocumentIndexService indexService;

    private final SystemUserClient systemUserClient;

    public PageResult<PolicyDocumentVO> page(PolicyDocumentPageQuery query) {
        if (StringUtils.hasText(query.getStatus())) {
            DocumentStatus.ofNullable(query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            PageResult<PolicyDocumentVO> esResult = indexService.search(query).orElse(null);
            if (esResult != null) {
                return esResult;
            }
        }
        return mysqlPage(query);
    }

    private PageResult<PolicyDocumentVO> mysqlPage(PolicyDocumentPageQuery query) {
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
    public PolicyDocumentVO create(Long publisherId, String username, PolicyDocumentSaveRequest request) {
        ensurePolicyManager(username);
        PolicyDocument document = new PolicyDocument();
        fill(document, request);
        document.setPublisherId(publisherId);
        if (document.getStatus() == DocumentStatus.PUBLISHED) {
            document.setPublishedAt(LocalDateTime.now());
        }
        documentMapper.insert(document);
        indexService.sync(document);
        return toVO(document);
    }

    @Transactional
    public PolicyDocumentVO upload(Long publisherId, String username, PolicyDocumentUploadRequest request, MultipartFile file) {
        ensurePolicyManager(username);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("policy file is empty");
        }
        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "policy";
        String content = parsePolicyFile(file, originalName);
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("policy file content is empty");
        }
        PolicyDocument document = new PolicyDocument();
        document.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle() : titleFromFilename(originalName));
        document.setContent(content);
        document.setSummary(summary(content));
        document.setDocumentVersion(StringUtils.hasText(request.getDocumentVersion()) ? request.getDocumentVersion() : "v1.0");
        document.setStatus(StringUtils.hasText(request.getStatus()) ? DocumentStatus.ofNullable(request.getStatus()) : DocumentStatus.DRAFT);
        document.setPublisherId(publisherId);
        if (document.getStatus() == DocumentStatus.PUBLISHED) {
            document.setPublishedAt(LocalDateTime.now());
        }
        documentMapper.insert(document);
        indexService.sync(document);
        return toVO(document);
    }

    @Transactional
    public PolicyDocumentVO update(String username, Long id, PolicyDocumentSaveRequest request) {
        ensurePolicyManager(username);
        PolicyDocument document = requireDocument(id);
        fill(document, request);
        if (document.getStatus() == DocumentStatus.PUBLISHED && document.getPublishedAt() == null) {
            document.setPublishedAt(LocalDateTime.now());
        }
        documentMapper.updateById(document);
        PolicyDocument updated = requireDocument(id);
        indexService.sync(updated);
        return toVO(updated);
    }

    public PolicyDocumentVO detail(Long id) {
        return toVO(requireDocument(id));
    }

    @Transactional
    public void delete(String username, Long id) {
        ensurePolicyManager(username);
        requireDocument(id);
        documentMapper.deleteById(id);
        indexService.delete(id);
    }

    public int reindex() {
        return indexService.rebuild(documentMapper.selectList(new LambdaQueryWrapper<PolicyDocument>()
                .orderByDesc(PolicyDocument::getUpdateTime)));
    }

    private void fill(PolicyDocument document, PolicyDocumentSaveRequest request) {
        document.setTitle(request.getTitle());
        document.setContent(request.getContent());
        document.setSummary(request.getSummary());
        document.setDocumentVersion(request.getDocumentVersion());
        document.setStatus(StringUtils.hasText(request.getStatus()) ? DocumentStatus.ofNullable(request.getStatus()) : DocumentStatus.DRAFT);
    }

    private String parsePolicyFile(MultipartFile file, String originalName) {
        String lowerName = originalName.toLowerCase(Locale.ROOT);
        try {
            if (lowerName.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(file.getContentType())) {
                try (PDDocument document = PDDocument.load(file.getInputStream())) {
                    return new PDFTextStripper().getText(document).trim();
                }
            }
            if (lowerName.endsWith(".txt") || lowerName.endsWith(".md")
                    || "text/plain".equalsIgnoreCase(file.getContentType())
                    || "text/markdown".equalsIgnoreCase(file.getContentType())) {
                return new String(file.getBytes(), StandardCharsets.UTF_8).trim();
            }
        } catch (Exception ex) {
            throw new BusinessException("parse policy file failed");
        }
        throw new BusinessException("only PDF, TXT and Markdown policy files are supported");
    }

    private String titleFromFilename(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        String title = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        return StringUtils.hasText(title) ? title : "Untitled policy";
    }

    private String summary(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 160) {
            return normalized;
        }
        return normalized.substring(0, 160) + "...";
    }

    private void ensurePolicyManager(String username) {
        Result<SystemAuthUserDTO> result = systemUserClient.getByUsername(username);
        if (result == null || result.code() != ErrorCode.SUCCESS.getCode() || result.data() == null) {
            throw new BusinessException("user not found");
        }
        if (result.data().roles() == null || result.data().roles().stream()
                .noneMatch(role -> "ADMIN".equals(role) || "MANAGER".equals(role))) {
            throw new BusinessException("permission denied");
        }
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
