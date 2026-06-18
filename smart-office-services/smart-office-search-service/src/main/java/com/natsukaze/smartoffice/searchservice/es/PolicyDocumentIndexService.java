package com.natsukaze.smartoffice.searchservice.es;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.natsukaze.smartoffice.common.core.PageResult;
import com.natsukaze.smartoffice.searchservice.config.SearchElasticsearchProperties;
import com.natsukaze.smartoffice.searchservice.dto.PolicyDocumentPageQuery;
import com.natsukaze.smartoffice.searchservice.entity.PolicyDocument;
import com.natsukaze.smartoffice.searchservice.vo.PolicyDocumentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyDocumentIndexService {

    private final ElasticsearchOperations operations;

    private final SearchElasticsearchProperties properties;

    private final ObjectMapper objectMapper;

    public void sync(PolicyDocument document) {
        if (!properties.isEnabled() || document == null || document.getId() == null) {
            return;
        }
        try {
            ensureIndex();
            operations.save(toIndex(document));
        } catch (RuntimeException ex) {
            log.warn("sync policy document index failed, documentId={}", document == null ? null : document.getId(), ex);
        }
    }

    public void delete(Long documentId) {
        if (!properties.isEnabled() || documentId == null) {
            return;
        }
        try {
            operations.delete(String.valueOf(documentId), PolicyDocumentIndex.class);
        } catch (RuntimeException ex) {
            log.warn("delete policy document index failed, documentId={}", documentId, ex);
        }
    }

    public Optional<PageResult<PolicyDocumentVO>> search(PolicyDocumentPageQuery query) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        try {
            ensureIndex();
            StringQuery searchQuery = new StringQuery(buildSearchSource(query));
            searchQuery.setPageable(PageRequest.of(Math.toIntExact(query.getCurrent() - 1), Math.toIntExact(query.getSize())));
            SearchHits<PolicyDocumentIndex> hits = operations.search(searchQuery, PolicyDocumentIndex.class);
            List<PolicyDocumentVO> records = hits.stream()
                    .map(SearchHit::getContent)
                    .map(this::toVO)
                    .toList();
            return Optional.of(new PageResult<>(records, hits.getTotalHits(), query.getCurrent(), query.getSize()));
        } catch (RuntimeException ex) {
            log.warn("search policy document index failed, fallback to mysql, keyword={}", query.getKeyword(), ex);
            return Optional.empty();
        }
    }

    public int rebuild(List<PolicyDocument> documents) {
        if (!properties.isEnabled()) {
            return 0;
        }
        try {
            IndexOperations indexOperations = operations.indexOps(PolicyDocumentIndex.class);
            if (indexOperations.exists()) {
                indexOperations.delete();
            }
            indexOperations.createWithMapping();
            List<PolicyDocumentIndex> indexes = documents.stream()
                    .map(this::toIndex)
                    .toList();
            indexes.forEach(operations::save);
            return indexes.size();
        } catch (RuntimeException ex) {
            log.warn("rebuild policy document index failed", ex);
            return 0;
        }
    }

    private void ensureIndex() {
        IndexOperations indexOperations = operations.indexOps(PolicyDocumentIndex.class);
        if (!indexOperations.exists()) {
            indexOperations.createWithMapping();
        }
    }

    private String buildSearchSource(PolicyDocumentPageQuery query) {
        List<Object> must = new ArrayList<>();
        List<Object> filter = new ArrayList<>();

        if (StringUtils.hasText(query.getKeyword())) {
            must.add(Map.of("multi_match", Map.of(
                    "query", query.getKeyword(),
                    "fields", List.of("title^3", "summary^2", "content", "documentVersion")
            )));
        } else {
            must.add(Map.of("match_all", Map.of()));
        }

        if (StringUtils.hasText(query.getStatus())) {
            filter.add(Map.of("term", Map.of("status", query.getStatus().toUpperCase())));
        }

        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put("must", must);
        if (!filter.isEmpty()) {
            bool.put("filter", filter);
        }

        Map<String, Object> source = new LinkedHashMap<>();
        source.put("query", Map.of("bool", bool));
        source.put("sort", List.of(
                Map.of("updateTime", Map.of("order", "desc")),
                Map.of("documentId", Map.of("order", "desc"))
        ));

        try {
            return objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("build policy document search query failed", ex);
        }
    }

    private PolicyDocumentIndex toIndex(PolicyDocument document) {
        return PolicyDocumentIndex.builder()
                .id(String.valueOf(document.getId()))
                .documentId(document.getId())
                .title(document.getTitle())
                .content(document.getContent())
                .summary(document.getSummary())
                .documentVersion(document.getDocumentVersion())
                .status(document.getStatus() == null ? null : document.getStatus().getCode())
                .publisherId(document.getPublisherId())
                .publishedAt(document.getPublishedAt())
                .createTime(document.getCreateTime())
                .updateTime(document.getUpdateTime())
                .build();
    }

    private PolicyDocumentVO toVO(PolicyDocumentIndex document) {
        return PolicyDocumentVO.builder()
                .id(document.getDocumentId())
                .title(document.getTitle())
                .content(document.getContent())
                .summary(document.getSummary())
                .documentVersion(document.getDocumentVersion())
                .status(document.getStatus())
                .publisherId(document.getPublisherId())
                .publishedAt(document.getPublishedAt())
                .build();
    }
}
