package com.recruit.agent.search.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.recruit.agent.position.model.PositionJD;
import com.recruit.agent.position.repository.PositionJDRepository;
import com.recruit.agent.rag.model.CandidateProfileIndex;
import com.recruit.agent.rag.model.ResumeChunk;
import com.recruit.agent.rag.embedding.EmbeddingService;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.normalization.PreparedCandidateSearchRequest;
import com.recruit.agent.search.normalization.SearchRequestNormalizationService;
import com.recruit.agent.search.reason.CandidateMatchReasonService;
import com.recruit.agent.search.rerank.CandidateSearchRerankService;
import com.recruit.agent.search.service.CandidateSearchService;
import com.recruit.agent.search.vo.CandidateSearchEvidenceVO;
import com.recruit.agent.search.vo.CandidateSearchItemVO;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

@Service
public class CandidateSearchServiceImpl implements CandidateSearchService {

    private static final int RRF_WINDOW_SIZE = 60;
    private static final int VECTOR_CANDIDATE_LIMIT_MULTIPLIER = 3;
    private static final Logger log = LoggerFactory.getLogger(CandidateSearchServiceImpl.class);

    private final ElasticsearchOperations elasticsearchOperations;
    private final EmbeddingService embeddingService;
    private final SearchRequestNormalizationService searchRequestNormalizationService;
    private final CandidateMatchReasonService candidateMatchReasonService;
    private final CandidateSearchRerankService candidateSearchRerankService;
    private final PositionJDRepository positionJDRepository;

    public CandidateSearchServiceImpl(ElasticsearchOperations elasticsearchOperations,
                                      EmbeddingService embeddingService,
                                      SearchRequestNormalizationService searchRequestNormalizationService,
                                      CandidateMatchReasonService candidateMatchReasonService,
                                      CandidateSearchRerankService candidateSearchRerankService,
                                      PositionJDRepository positionJDRepository) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.embeddingService = embeddingService;
        this.searchRequestNormalizationService = searchRequestNormalizationService;
        this.candidateMatchReasonService = candidateMatchReasonService;
        this.candidateSearchRerankService = candidateSearchRerankService;
        this.positionJDRepository = positionJDRepository;
    }

    @Override
    public CandidateSearchResponse search(CandidateSearchRequest request) {
        PreparedCandidateSearchRequest prepared = searchRequestNormalizationService.prepare(request);
        String query = safeText(prepared.getQuery());
        CandidateSearchFilter filter = prepared.getFilter() == null ? new CandidateSearchFilter() : prepared.getFilter();
        List<String> queryTerms = tokenize(query);
        Optional<float[]> queryEmbedding = resolveQueryEmbedding(query);

        SearchHits<CandidateProfileIndex> keywordHits = elasticsearchOperations.search(
            buildCandidateSearchQuery(query, filter, prepared.getScopeCandidateIds(), queryTerms, prepared.getLimit()),
            CandidateProfileIndex.class
        );

        List<CandidateSearchHit> mergedHits = mergeHybridHits(
            keywordHits,
            queryEmbedding,
            query,
            filter,
            prepared.getScopeCandidateIds(),
            prepared.getLimit()
        );

        List<CandidateSearchItemVO> candidates = mergedHits.stream()
            .map(hit -> toItem(hit, query, queryTerms, filter, prepared.getEvidenceLimit()))
            .toList();
        candidates = candidateSearchRerankService.rerank(query, candidates, resolvePositionJd(request));
        assignSearchRanks(candidates);

        CandidateSearchResponse response = new CandidateSearchResponse();
        response.setQuery(query);
        response.setTotal(mergedHits.size());
        response.setCandidates(candidates);
        return response;
    }

    private List<CandidateSearchHit> mergeHybridHits(SearchHits<CandidateProfileIndex> keywordHits,
                                                     Optional<float[]> queryEmbedding,
                                                     String query,
                                                     CandidateSearchFilter filter,
                                                     List<String> scopeCandidateIds,
                                                     int limit) {
        List<CandidateSearchHit> keywordCandidates = keywordHits.getSearchHits().stream()
            .map(hit -> new CandidateSearchHit(hit.getContent(), resolveMatchScore(hit)))
            .toList();

        if (query.isBlank() || queryEmbedding.isEmpty()) {
            return keywordCandidates;
        }

        List<CandidateSearchHit> profileVectorCandidates = loadProfileVectorCandidates(queryEmbedding.get(), filter, scopeCandidateIds, limit);
        List<CandidateSearchHit> chunkVectorCandidates = loadChunkVectorCandidates(queryEmbedding.get(), filter, scopeCandidateIds, limit);
        if (profileVectorCandidates.isEmpty() && chunkVectorCandidates.isEmpty()) {
            return keywordCandidates;
        }

        return fuseCandidates(keywordCandidates, profileVectorCandidates, chunkVectorCandidates, limit);
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
        } else {
            builder.withSort(sort -> sort.score(score -> score.order(SortOrder.Desc)));
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

        if (filter.getCareerStage() != null) {
            filters.add(Query.of(q -> q.term(t -> t
                .field("careerStage")
                .value(FieldValue.of(filter.getCareerStage().name())))));
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
        return toItem(new CandidateSearchHit(hit.getContent(), resolveMatchScore(hit)), query, queryTerms, filter, evidenceLimit);
    }

    private CandidateSearchItemVO toItem(CandidateSearchHit hit,
                                         String query,
                                         List<String> queryTerms,
                                         CandidateSearchFilter filter,
                                         int evidenceLimit) {
        CandidateProfileIndex profile = hit.profile();
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
        item.setMatchScore(hit.score());
        item.setMatchReasons(candidateMatchReasonService.buildMatchReasons(profile, query, queryTerms, filter));
        item.setEvidenceList(loadEvidence(profile.getCandidateId(), query, queryTerms, evidenceLimit));
        return item;
    }

    private void assignSearchRanks(List<CandidateSearchItemVO> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        for (int index = 0; index < candidates.size(); index++) {
            candidates.get(index).setRank(index + 1);
        }
    }

    private double resolveMatchScore(SearchHit<?> hit) {
        if (hit == null) {
            return 0.0d;
        }
        double score = hit.getScore();
        return Double.isFinite(score) ? score : 0.0d;
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

    private List<CandidateSearchHit> loadProfileVectorCandidates(float[] queryEmbedding,
                                                                 CandidateSearchFilter filter,
                                                                 List<String> scopeCandidateIds,
                                                                 int limit) {
        SearchHits<CandidateProfileIndex> profileVectorHits;
        try {
            profileVectorHits = elasticsearchOperations.search(
                buildVectorProfileSearchQuery(queryEmbedding, filter, scopeCandidateIds, limit),
                CandidateProfileIndex.class
            );
        } catch (RuntimeException exception) {
            log.warn("Candidate profile vector recall failed. Fallback to non-profile-vector retrieval.", exception);
            return List.of();
        }

        return profileVectorHits.getSearchHits().stream()
            .map(hit -> new CandidateSearchHit(hit.getContent(), resolveMatchScore(hit)))
            .toList();
    }

    private List<CandidateSearchHit> loadChunkVectorCandidates(float[] queryEmbedding,
                                                               CandidateSearchFilter filter,
                                                               List<String> scopeCandidateIds,
                                                               int limit) {
        SearchHits<ResumeChunk> vectorHits;
        try {
            vectorHits = elasticsearchOperations.search(
                buildVectorChunkSearchQuery(queryEmbedding, scopeCandidateIds, limit * VECTOR_CANDIDATE_LIMIT_MULTIPLIER),
                ResumeChunk.class
            );
        } catch (RuntimeException exception) {
            log.warn("Resume chunk vector recall failed. Fallback to non-chunk-vector retrieval.", exception);
            return List.of();
        }

        Map<String, Double> candidateScores = aggregateCandidateVectorScores(vectorHits);
        if (candidateScores.isEmpty()) {
            return List.of();
        }

        SearchHits<CandidateProfileIndex> profileHits = elasticsearchOperations.search(
            buildCandidateIdsQuery(new ArrayList<>(candidateScores.keySet()), filter),
            CandidateProfileIndex.class
        );

        return profileHits.getSearchHits().stream()
            .map(SearchHit::getContent)
            .map(profile -> new CandidateSearchHit(
                profile,
                normalizeScore(candidateScores.get(profile.getCandidateId()))))
            .sorted(Comparator.comparing(CandidateSearchHit::score).reversed()
                .thenComparing(hit -> safeText(hit.profile().getCandidateNo())))
            .limit(limit)
            .toList();
    }

    private org.springframework.data.elasticsearch.core.query.Query buildVectorProfileSearchQuery(float[] queryEmbedding,
                                                                                                   CandidateSearchFilter filter,
                                                                                                   List<String> scopeCandidateIds,
                                                                                                   int limit) {
        String baseQueryJson = buildCandidateFilterJson(filter, scopeCandidateIds);
        String vectorJson = buildVectorJson(queryEmbedding);
        String queryJson = """
            {
              "script_score": {
                "query": %s,
                "script": {
                  "source": "cosineSimilarity(params.queryVector, 'embedding') + 1.0",
                  "params": {
                    "queryVector": %s
                  }
                }
              }
            }
            """.formatted(baseQueryJson, vectorJson);

        StringQuery query = new StringQuery(queryJson);
        query.setPageable(PageRequest.of(0, limit));
        return query;
    }

    private org.springframework.data.elasticsearch.core.query.Query buildVectorChunkSearchQuery(float[] queryEmbedding,
                                                                                                 List<String> scopeCandidateIds,
                                                                                                 int limit) {
        String baseQueryJson = buildVectorBaseQueryJson(scopeCandidateIds);
        String vectorJson = buildVectorJson(queryEmbedding);
        String queryJson = """
            {
              "script_score": {
                "query": %s,
                "script": {
                  "source": "cosineSimilarity(params.queryVector, 'embedding') + 1.0",
                  "params": {
                    "queryVector": %s
                  }
                }
              }
            }
            """.formatted(baseQueryJson, vectorJson);

        StringQuery query = new StringQuery(queryJson);
        query.setPageable(PageRequest.of(0, limit));
        return query;
    }

    private NativeQuery buildCandidateIdsQuery(List<String> candidateIds, CandidateSearchFilter filter) {
        return new NativeQueryBuilder()
            .withQuery(Query.of(q -> q.bool(b -> b.filter(buildCandidateFilters(filter, candidateIds)))))
            .withPageable(PageRequest.of(0, Math.max(candidateIds.size(), 1)))
            .build();
    }

    private List<Query> buildCandidateFilters(CandidateSearchFilter filter, List<String> candidateIds) {
        List<Query> filters = new ArrayList<>();
        if (candidateIds != null && !candidateIds.isEmpty()) {
            filters.add(Query.of(q -> q.terms(t -> t
                .field("candidateId")
                .terms(values -> values.value(candidateIds.stream()
                    .filter(Objects::nonNull)
                    .map(FieldValue::of)
                    .toList())))));
        }

        if (filter.getCareerStage() != null) {
            filters.add(Query.of(q -> q.term(t -> t
                .field("careerStage")
                .value(FieldValue.of(filter.getCareerStage().name())))));
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
        return filters;
    }

    private Map<String, Double> aggregateCandidateVectorScores(SearchHits<ResumeChunk> vectorHits) {
        Map<String, Double> aggregated = new LinkedHashMap<>();
        if (vectorHits == null || vectorHits.getSearchHits() == null) {
            return aggregated;
        }
        for (SearchHit<ResumeChunk> hit : vectorHits.getSearchHits()) {
            ResumeChunk chunk = hit.getContent();
            if (chunk == null || chunk.getCandidateId() == null || chunk.getCandidateId().isBlank()) {
                continue;
            }
            double score = resolveMatchScore(hit);
            aggregated.merge(chunk.getCandidateId(), score, Math::max);
        }
        return aggregated;
    }

    private List<CandidateSearchHit> fuseCandidates(List<CandidateSearchHit> keywordCandidates,
                                                    List<CandidateSearchHit> profileVectorCandidates,
                                                    List<CandidateSearchHit> chunkVectorCandidates,
                                                    int limit) {
        Map<String, CandidateFusionState> states = new LinkedHashMap<>();

        applyRrf(states, keywordCandidates);
        applyRrf(states, profileVectorCandidates);
        applyRrf(states, chunkVectorCandidates);

        return states.values().stream()
            .sorted(Comparator.comparing(CandidateFusionState::fusedScore).reversed()
                .thenComparing(state -> safeText(state.profile().getCandidateNo())))
            .limit(limit)
            .map(state -> new CandidateSearchHit(state.profile(), state.fusedScore()))
            .toList();
    }

    private void applyRrf(Map<String, CandidateFusionState> states, List<CandidateSearchHit> hits) {
        for (int index = 0; index < hits.size(); index++) {
            CandidateSearchHit hit = hits.get(index);
            String candidateId = hit.profile().getCandidateId();
            if (candidateId == null || candidateId.isBlank()) {
                continue;
            }
            CandidateFusionState state = states.computeIfAbsent(candidateId,
                ignored -> new CandidateFusionState(hit.profile(), 0.0d));
            state.addRrf(1.0d / (RRF_WINDOW_SIZE + index + 1));
        }
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

    private List<String> tokenize(String query) {
        String normalized = safeText(query).trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        return Arrays.stream(normalized.toLowerCase(Locale.ROOT).split("[\\s,，、]+"))
            .filter(token -> !token.isBlank())
            .toList();
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    private PositionJD resolvePositionJd(CandidateSearchRequest request) {
        if (request == null || request.getPositionId() == null || request.getPositionId().isBlank()) {
            return null;
        }
        String positionId = request.getPositionId().trim();
        return positionJDRepository.findById(positionId)
            .or(() -> positionJDRepository.findByJdNo(positionId))
            .orElse(null);
    }

    private String buildVectorBaseQueryJson(List<String> scopeCandidateIds) {
        if (scopeCandidateIds == null || scopeCandidateIds.isEmpty()) {
            return "{\"match_all\":{}}";
        }
        String idsJson = scopeCandidateIds.stream()
            .filter(Objects::nonNull)
            .map(this::toJsonString)
            .reduce((left, right) -> left + "," + right)
            .orElse("");
        return """
            {
              "bool": {
                "filter": [
                  {
                    "terms": {
                      "candidateId": [%s]
                    }
                  }
                ]
              }
            }
            """.formatted(idsJson);
    }

    private String buildCandidateFilterJson(CandidateSearchFilter filter, List<String> scopeCandidateIds) {
        List<String> clauses = new ArrayList<>();
        if (scopeCandidateIds != null && !scopeCandidateIds.isEmpty()) {
            String idsJson = scopeCandidateIds.stream()
                .filter(Objects::nonNull)
                .map(this::toJsonString)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
            clauses.add("""
                {
                  "terms": {
                    "candidateId": [%s]
                  }
                }
                """.formatted(idsJson));
        }
        if (filter.getCareerStage() != null) {
            clauses.add("""
                {
                  "term": {
                    "careerStage": %s
                  }
                }
                """.formatted(toJsonString(filter.getCareerStage().name())));
        }
        if (filter.getHighestDegrees() != null && !filter.getHighestDegrees().isEmpty()) {
            String values = filter.getHighestDegrees().stream()
                .map(Enum::name)
                .map(this::toJsonString)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
            clauses.add("""
                {
                  "terms": {
                    "highestDegree": [%s]
                  }
                }
                """.formatted(values));
        }
        if (filter.getSchoolTiers() != null && !filter.getSchoolTiers().isEmpty()) {
            String values = filter.getSchoolTiers().stream()
                .map(Enum::name)
                .map(this::toJsonString)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
            clauses.add("""
                {
                  "terms": {
                    "schoolTier": [%s]
                  }
                }
                """.formatted(values));
        }
        if (filter.getMinYearsOfExperience() != null) {
            clauses.add("""
                {
                  "range": {
                    "totalYearsOfExperience": {
                      "gte": %s
                    }
                  }
                }
                """.formatted(filter.getMinYearsOfExperience()));
        }
        if (filter.getTechnicalSkills() != null && !filter.getTechnicalSkills().isEmpty()) {
            filter.getTechnicalSkills().stream()
                .map(this::toJsonString)
                .map(value -> """
                    {
                      "term": {
                        "technicalSkills": %s
                      }
                    }
                    """.formatted(value))
                .forEach(clauses::add);
        }
        if (filter.getCurrentCity() != null && !filter.getCurrentCity().isBlank()) {
            clauses.add("""
                {
                  "term": {
                    "currentCity": %s
                  }
                }
                """.formatted(toJsonString(filter.getCurrentCity())));
        }
        if (filter.getBigTech() != null) {
            clauses.add("""
                {
                  "term": {
                    "bigTech": %s
                  }
                }
                """.formatted(filter.getBigTech()));
        }
        if (filter.getOutsourcing() != null) {
            clauses.add("""
                {
                  "term": {
                    "outsourcing": %s
                  }
                }
                """.formatted(filter.getOutsourcing()));
        }
        if (clauses.isEmpty()) {
            return "{\"match_all\":{}}";
        }
        return """
            {
              "bool": {
                "filter": [%s]
              }
            }
            """.formatted(String.join(",", clauses));
    }

    private String buildVectorJson(float[] queryEmbedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < queryEmbedding.length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(queryEmbedding[index]);
        }
        builder.append(']');
        return builder.toString();
    }

    private String toJsonString(String value) {
        String safe = value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + safe + "\"";
    }

    private Optional<float[]> resolveQueryEmbedding(String query) {
        if (query == null || query.isBlank() || !embeddingService.isAvailable()) {
            return Optional.empty();
        }
        try {
            List<float[]> embeddings = embeddingService.embedAll(List.of(query));
            if (embeddings == null || embeddings.isEmpty() || embeddings.get(0) == null || embeddings.get(0).length == 0) {
                return Optional.empty();
            }
            return Optional.of(embeddings.get(0));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private double normalizeScore(Double score) {
        return score != null && Double.isFinite(score) ? score : 0.0d;
    }

    private record CandidateSearchHit(CandidateProfileIndex profile, double score) {
    }

    private static final class CandidateFusionState {

        private final CandidateProfileIndex profile;
        private double fusedScore;

        private CandidateFusionState(CandidateProfileIndex profile, double fusedScore) {
            this.profile = profile;
            this.fusedScore = fusedScore;
        }

        private CandidateProfileIndex profile() {
            return profile;
        }

        private double fusedScore() {
            return fusedScore;
        }

        private void addRrf(double delta) {
            this.fusedScore += delta;
        }
    }
}
