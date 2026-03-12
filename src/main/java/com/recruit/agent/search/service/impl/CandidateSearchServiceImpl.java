package com.recruit.agent.search.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.recruit.agent.rag.model.CandidateProfileIndex;
import com.recruit.agent.rag.model.ResumeChunk;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.parser.NaturalLanguageSearchFilterParser;
import com.recruit.agent.search.service.CandidateSearchService;
import com.recruit.agent.search.vo.CandidateSearchEvidenceVO;
import com.recruit.agent.search.vo.CandidateSearchItemVO;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

/**
 * 候选人搜索服务实现。
 */
@Service
public class CandidateSearchServiceImpl implements CandidateSearchService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int DEFAULT_EVIDENCE_LIMIT = 3;

    private final ElasticsearchOperations elasticsearchOperations;
    private final NaturalLanguageSearchFilterParser naturalLanguageSearchFilterParser;

    public CandidateSearchServiceImpl(ElasticsearchOperations elasticsearchOperations,
                                      NaturalLanguageSearchFilterParser naturalLanguageSearchFilterParser) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.naturalLanguageSearchFilterParser = naturalLanguageSearchFilterParser;
    }

    @Override
    public CandidateSearchResponse search(CandidateSearchRequest request) {
        CandidateSearchRequest safeRequest = request == null ? new CandidateSearchRequest() : request;
        String rawQuery = safeText(safeRequest.getQuery());
        String query = safeText(naturalLanguageSearchFilterParser.stripFilterTerms(rawQuery));
        CandidateSearchFilter filter = mergeFilter(
            safeRequest.getFilter() == null ? new CandidateSearchFilter() : safeRequest.getFilter(),
            naturalLanguageSearchFilterParser.parse(rawQuery)
        );
        List<String> queryTerms = tokenize(query);
        int limit = normalizeCandidateLimit(safeRequest.getLimit());
        int evidenceLimit = normalizeEvidenceLimit(safeRequest.getEvidenceLimit());

        SearchHits<CandidateProfileIndex> searchHits = elasticsearchOperations.search(
            buildCandidateSearchQuery(query, filter, safeRequest.getScopeCandidateIds(), queryTerms, limit),
            CandidateProfileIndex.class
        );

        List<CandidateSearchItemVO> candidates = searchHits.getSearchHits().stream()
            .map(hit -> toItem(hit, query, queryTerms, filter, evidenceLimit))
            .toList();

        CandidateSearchResponse response = new CandidateSearchResponse();
        response.setQuery(query);
        response.setTotal(Math.toIntExact(searchHits.getTotalHits()));
        response.setCandidates(candidates);
        return response;
    }

    private NativeQuery buildCandidateSearchQuery(String query,
                                                  CandidateSearchFilter filter,
                                                  List<String> scopeCandidateIds,
                                                  List<String> queryTerms,
                                                  int limit) {
        NativeQueryBuilder builder = new NativeQueryBuilder()
            .withQuery(buildCandidateQueryDsl(query, filter, scopeCandidateIds, queryTerms))
            .withPageable(PageRequest.of(0, limit))
            .withTrackTotalHits(true);

        if (query.isBlank()) {
            builder.withSort(sort -> sort.field(field -> field.field("totalYearsOfExperience").order(SortOrder.Desc)));
        }
        builder.withSort(sort -> sort.field(field -> field.field("candidateNo").order(SortOrder.Asc)));
        return builder.build();
    }

    private Query buildCandidateQueryDsl(String query,
                                         CandidateSearchFilter filter,
                                         List<String> scopeCandidateIds,
                                         List<String> queryTerms) {
        List<Query> filters = new ArrayList<>();
        if (scopeCandidateIds != null && !scopeCandidateIds.isEmpty()) {
            filters.add(Query.of(q -> q.terms(t -> t
                .field("candidateId")
                .terms(values -> values.value(scopeCandidateIds.stream()
                    .filter(Objects::nonNull)
                    .map(FieldValue::of)
                    .toList())))));
        }
        if (filter.getHighestDegrees() != null && !filter.getHighestDegrees().isEmpty()) {
            filters.add(Query.of(q -> q.terms(t -> t
                .field("highestDegree")
                .terms(values -> values.value(filter.getHighestDegrees().stream()
                    .map(Enum::name)
                    .map(FieldValue::of)
                    .toList())))));
        }

        if (filter.getSchoolTiers() != null && !filter.getSchoolTiers().isEmpty()) {
            filters.add(Query.of(q -> q.terms(t -> t
                .field("schoolTier")
                .terms(values -> values.value(filter.getSchoolTiers().stream()
                    .map(Enum::name)
                    .map(FieldValue::of)
                    .toList())))));
        }

        if (filter.getMinYearsOfExperience() != null) {
            filters.add(Query.of(q -> q.range(r -> r
                .number(n -> n
                    .field("totalYearsOfExperience")
                    .gte(filter.getMinYearsOfExperience().doubleValue())))));
        }

        if (filter.getTechnicalSkills() != null && !filter.getTechnicalSkills().isEmpty()) {
            filter.getTechnicalSkills().forEach(skill -> filters.add(Query.of(q -> q.term(t -> t
                .field("technicalSkills")
                .value(FieldValue.of(skill))))));
        }

        if (filter.getCurrentCity() != null && !filter.getCurrentCity().isBlank()) {
            filters.add(Query.of(q -> q.term(t -> t
                .field("currentCity")
                .value(FieldValue.of(filter.getCurrentCity())))));
        }

        if (filter.getBigTech() != null) {
            filters.add(Query.of(q -> q.term(t -> t
                .field("bigTech")
                .value(FieldValue.of(filter.getBigTech())))));
        }

        if (filter.getOutsourcing() != null) {
            filters.add(Query.of(q -> q.term(t -> t
                .field("outsourcing")
                .value(FieldValue.of(filter.getOutsourcing())))));
        }

        if (query.isBlank()) {
            return Query.of(q -> q.bool(b -> b.filter(filters)));
        }

        List<Query> shouldQueries = new ArrayList<>();
        shouldQueries.add(Query.of(q -> q.multiMatch(m -> m
            .query(query)
            .fields("fullName^3", "profileSummary^2"))));

        for (String term : queryTerms) {
            if (term.isBlank()) {
                continue;
            }
            shouldQueries.add(Query.of(q -> q.term(t -> t.field("technicalSkills").value(FieldValue.of(term)))));
            shouldQueries.add(Query.of(q -> q.term(t -> t.field("projectTags").value(FieldValue.of(term)))));
            shouldQueries.add(Query.of(q -> q.term(t -> t.field("companyTags").value(FieldValue.of(term)))));
            shouldQueries.add(Query.of(q -> q.term(t -> t.field("industryTags").value(FieldValue.of(term)))));
        }

        return Query.of(q -> q.bool(b -> b
            .filter(filters)
            .should(shouldQueries)
            .minimumShouldMatch("1")));
    }

    private CandidateSearchItemVO toItem(SearchHit<CandidateProfileIndex> hit,
                                         String query,
                                         List<String> queryTerms,
                                         CandidateSearchFilter filter,
                                         int evidenceLimit) {
        CandidateProfileIndex profile = hit.getContent();
        CandidateSearchItemVO item = new CandidateSearchItemVO();
        item.setCandidateId(profile.getCandidateId());
        item.setCandidateNo(profile.getCandidateNo());
        item.setFullName(profile.getFullName());
        item.setCurrentCity(profile.getCurrentCity());
        item.setSchoolName(profile.getSchoolName());
        item.setHighestDegree(profile.getHighestDegree());
        item.setSchoolTier(profile.getSchoolTier());
        item.setTotalYearsOfExperience(profile.getTotalYearsOfExperience());
        item.setTechnicalSkills(profile.getTechnicalSkills());
        item.setBigTech(profile.getBigTech());
        item.setOutsourcing(profile.getOutsourcing());
        item.setProfileSummary(profile.getProfileSummary());
        item.setMatchScore(hit.getScore());
        item.setMatchReasons(buildMatchReasons(profile, query, queryTerms, filter));
        item.setEvidenceList(loadEvidence(profile.getCandidateId(), query, queryTerms, evidenceLimit));
        return item;
    }

    private List<String> buildMatchReasons(CandidateProfileIndex profile,
                                           String query,
                                           List<String> queryTerms,
                                           CandidateSearchFilter filter) {
        List<String> reasons = new ArrayList<>();

        if (!query.isBlank() && containsAny(normalize(query), profile.getTechnicalSkills())) {
            reasons.add("技术栈命中查询关键词");
        }
        if (!query.isBlank() && containsAny(normalize(query), profile.getProjectTags())) {
            reasons.add("项目标签命中查询关键词");
        }
        if (!query.isBlank() && normalize(profile.getProfileSummary()).contains(normalize(query))) {
            reasons.add("候选人摘要与查询语义匹配");
        }
        if (filter.getMinYearsOfExperience() != null && profile.getTotalYearsOfExperience() != null) {
            reasons.add("工作年限满足筛选条件");
        }
        if (Boolean.TRUE.equals(filter.getBigTech()) && Boolean.TRUE.equals(profile.getBigTech())) {
            reasons.add("具备大厂背景");
        }
        if (filter.getTechnicalSkills() != null && !filter.getTechnicalSkills().isEmpty()) {
            reasons.add("满足技术栈过滤条件");
        }

        if (reasons.isEmpty() && !queryTerms.isEmpty()) {
            reasons.add("基础关键词匹配");
        }
        if (reasons.isEmpty()) {
            reasons.add("满足结构化过滤条件");
        }
        return reasons;
    }

    private List<CandidateSearchEvidenceVO> loadEvidence(String candidateId,
                                                         String query,
                                                         List<String> queryTerms,
                                                         int evidenceLimit) {
        if (evidenceLimit <= 0) {
            return List.of();
        }

        SearchHits<ResumeChunk> searchHits = elasticsearchOperations.search(
            buildEvidenceSearchQuery(candidateId, query, queryTerms, evidenceLimit),
            ResumeChunk.class
        );

        return searchHits.getSearchHits().stream()
            .map(SearchHit::getContent)
            .map(this::toEvidence)
            .toList();
    }

    private NativeQuery buildEvidenceSearchQuery(String candidateId,
                                                 String query,
                                                 List<String> queryTerms,
                                                 int evidenceLimit) {
        return new NativeQueryBuilder()
            .withQuery(buildEvidenceQueryDsl(candidateId, query, queryTerms))
            .withPageable(PageRequest.of(0, evidenceLimit))
            .build();
    }

    private Query buildEvidenceQueryDsl(String candidateId, String query, List<String> queryTerms) {
        List<Query> filters = List.of(Query.of(q -> q.term(t -> t
            .field("candidateId")
            .value(FieldValue.of(candidateId)))));

        if (query.isBlank()) {
            return Query.of(q -> q.bool(b -> b.filter(filters)));
        }

        List<Query> shouldQueries = new ArrayList<>();
        shouldQueries.add(Query.of(q -> q.multiMatch(m -> m
            .query(query)
            .fields("content^2", "normalizedContent"))));

        for (String term : queryTerms) {
            if (term.isBlank()) {
                continue;
            }
            shouldQueries.add(Query.of(q -> q.match(m -> m.field("content").query(term))));
        }

        return Query.of(q -> q.bool(b -> b
            .filter(filters)
            .should(shouldQueries)
            .minimumShouldMatch("1")));
    }

    private CandidateSearchEvidenceVO toEvidence(ResumeChunk chunk) {
        CandidateSearchEvidenceVO evidence = new CandidateSearchEvidenceVO();
        evidence.setChunkId(chunk.getId());
        evidence.setDocId(chunk.getDocId());
        evidence.setSection(chunk.getSection());
        evidence.setPage(chunk.getPage());
        evidence.setContent(chunk.getContent());
        return evidence;
    }

    private boolean containsAny(String queryTerm, List<String> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        return values.stream()
            .filter(Objects::nonNull)
            .map(this::normalize)
            .anyMatch(value -> value.contains(queryTerm));
    }

    private List<String> tokenize(String query) {
        String normalized = safeText(query).trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        return Arrays.stream(normalized.toLowerCase(Locale.ROOT).split("[\\s,，;；]+"))
            .filter(token -> !token.isBlank())
            .toList();
    }

    private String normalize(String text) {
        return safeText(text).toLowerCase(Locale.ROOT);
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    private int normalizeCandidateLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return limit;
    }

    private int normalizeEvidenceLimit(Integer limit) {
        if (limit == null || limit < 0) {
            return DEFAULT_EVIDENCE_LIMIT;
        }
        return limit;
    }

    private CandidateSearchFilter mergeFilter(CandidateSearchFilter explicitFilter, CandidateSearchFilter parsedFilter) {
        CandidateSearchFilter merged = new CandidateSearchFilter();
        merged.setHighestDegrees(mergeList(
            explicitFilter == null ? null : explicitFilter.getHighestDegrees(),
            parsedFilter == null ? null : parsedFilter.getHighestDegrees()
        ));
        merged.setSchoolTiers(mergeList(
            explicitFilter == null ? null : explicitFilter.getSchoolTiers(),
            parsedFilter == null ? null : parsedFilter.getSchoolTiers()
        ));
        merged.setTechnicalSkills(mergeList(
            explicitFilter == null ? null : explicitFilter.getTechnicalSkills(),
            parsedFilter == null ? null : parsedFilter.getTechnicalSkills()
        ));
        merged.setMinYearsOfExperience(max(
            explicitFilter == null ? null : explicitFilter.getMinYearsOfExperience(),
            parsedFilter == null ? null : parsedFilter.getMinYearsOfExperience()
        ));
        merged.setCurrentCity(hasText(explicitFilter == null ? null : explicitFilter.getCurrentCity())
            ? explicitFilter.getCurrentCity()
            : parsedFilter == null ? null : parsedFilter.getCurrentCity());
        merged.setBigTech(explicitFilter != null && explicitFilter.getBigTech() != null
            ? explicitFilter.getBigTech()
            : parsedFilter == null ? null : parsedFilter.getBigTech());
        merged.setOutsourcing(explicitFilter != null && explicitFilter.getOutsourcing() != null
            ? explicitFilter.getOutsourcing()
            : parsedFilter == null ? null : parsedFilter.getOutsourcing());
        return merged;
    }

    private <T> List<T> mergeList(List<T> left, List<T> right) {
        if ((left == null || left.isEmpty()) && (right == null || right.isEmpty())) {
            return null;
        }
        List<T> merged = new ArrayList<>();
        if (left != null) {
            merged.addAll(left);
        }
        if (right != null) {
            for (T value : right) {
                if (!merged.contains(value)) {
                    merged.add(value);
                }
            }
        }
        return merged;
    }

    private BigDecimal max(BigDecimal left, BigDecimal right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.compareTo(right) >= 0 ? left : right;
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}
