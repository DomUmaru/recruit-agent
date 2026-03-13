package com.recruit.agent.search.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import com.recruit.agent.rag.embedding.UnavailableEmbeddingService;
import com.recruit.agent.rag.model.CandidateProfileIndex;
import com.recruit.agent.rag.model.ResumeChunk;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.normalization.impl.SearchRequestNormalizationServiceImpl;
import com.recruit.agent.search.parser.impl.RuleBasedNaturalLanguageSearchFilterParser;
import com.recruit.agent.search.reason.impl.DefaultCandidateMatchReasonService;
import com.recruit.agent.search.rerank.impl.DefaultCandidateSearchRerankService;
import com.recruit.agent.search.rerank.UnavailableRerankService;
import com.recruit.agent.search.service.impl.CandidateSearchServiceImpl;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

class CandidateSearchServiceImplTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldSearchByQueryAndFilterAndReturnEvidence() {
        ElasticsearchOperations elasticsearchOperations = org.mockito.Mockito.mock(ElasticsearchOperations.class);
        CandidateSearchServiceImpl service = new CandidateSearchServiceImpl(
            elasticsearchOperations,
            new UnavailableEmbeddingService(),
            new SearchRequestNormalizationServiceImpl(new RuleBasedNaturalLanguageSearchFilterParser()),
            new DefaultCandidateMatchReasonService(),
            new DefaultCandidateSearchRerankService(new UnavailableRerankService())
        );

        CandidateProfileIndex matched = new CandidateProfileIndex();
        matched.setId("idx-1");
        matched.setCandidateId("candidate-1");
        matched.setCandidateNo("C-001");
        matched.setFullName("张三");
        matched.setCurrentCity("上海");
        matched.setHighestDegree(DegreeLevel.BACHELOR.name());
        matched.setSchoolTier(SchoolTier.PROJECT_985.name());
        matched.setTotalYearsOfExperience(new BigDecimal("5.0"));
        matched.setTechnicalSkills(List.of("Java", "Spring Boot", "Elasticsearch"));
        matched.setProjectTags(List.of("推荐系统"));
        matched.setBigTech(true);
        matched.setOutsourcing(false);
        matched.setProfileSummary("具备推荐系统和搜索相关经验，熟悉 Java、Elasticsearch。");

        ResumeChunk evidence = new ResumeChunk();
        evidence.setId("chunk-1");
        evidence.setCandidateId("candidate-1");
        evidence.setDocId("doc-1");
        evidence.setSection("project");
        evidence.setPage(1);
        evidence.setChunkOrder(1);
        evidence.setContent("负责推荐系统召回与排序模块，使用 Java、Spring Boot、Elasticsearch。");

        SearchHit<CandidateProfileIndex> candidateHit = org.mockito.Mockito.mock(SearchHit.class);
        when(candidateHit.getContent()).thenReturn(matched);
        when(candidateHit.getScore()).thenReturn(8.5f);

        SearchHits<CandidateProfileIndex> candidateHits = org.mockito.Mockito.mock(SearchHits.class);
        when(candidateHits.getSearchHits()).thenReturn(List.of(candidateHit));
        when(candidateHits.getTotalHits()).thenReturn(1L);

        SearchHits<ResumeChunk> resumeChunkHits = org.mockito.Mockito.mock(SearchHits.class);
        SearchHit<ResumeChunk> evidenceHit = org.mockito.Mockito.mock(SearchHit.class);
        when(evidenceHit.getContent()).thenReturn(evidence);
        when(resumeChunkHits.getSearchHits()).thenReturn(List.of(evidenceHit));

        when(elasticsearchOperations.search(any(Query.class), eq(CandidateProfileIndex.class))).thenReturn(candidateHits);
        when(elasticsearchOperations.search(any(Query.class), eq(ResumeChunk.class))).thenReturn(resumeChunkHits);

        CandidateSearchFilter filter = new CandidateSearchFilter();
        filter.setHighestDegrees(List.of(DegreeLevel.BACHELOR));
        filter.setSchoolTiers(List.of(SchoolTier.PROJECT_985));
        filter.setMinYearsOfExperience(new BigDecimal("3.0"));
        filter.setTechnicalSkills(List.of("Java"));
        filter.setBigTech(true);
        filter.setOutsourcing(false);

        CandidateSearchRequest request = new CandidateSearchRequest();
        request.setQuery("推荐系统 Java");
        request.setFilter(filter);
        request.setLimit(10);
        request.setEvidenceLimit(2);

        CandidateSearchResponse response = service.search(request);

        assertEquals(1, response.getTotal());
        assertEquals("candidate-1", response.getCandidates().get(0).getCandidateId());
        assertEquals("上海", response.getCandidates().get(0).getCurrentCity());
        assertTrue(Double.isFinite(response.getCandidates().get(0).getMatchScore()));
        assertFalse(response.getCandidates().get(0).getEvidenceList().isEmpty());
        assertFalse(response.getCandidates().get(0).getMatchReasons().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSupportFilterOnlySearchWhenQueryIsFullyStripped() {
        ElasticsearchOperations elasticsearchOperations = org.mockito.Mockito.mock(ElasticsearchOperations.class);
        CandidateSearchServiceImpl service = new CandidateSearchServiceImpl(
            elasticsearchOperations,
            new UnavailableEmbeddingService(),
            new SearchRequestNormalizationServiceImpl(new RuleBasedNaturalLanguageSearchFilterParser()),
            new DefaultCandidateMatchReasonService(),
            new DefaultCandidateSearchRerankService(new UnavailableRerankService())
        );

        CandidateProfileIndex matched = new CandidateProfileIndex();
        matched.setCandidateId("candidate-1");
        matched.setCandidateNo("C-001");
        matched.setFullName("张三");
        matched.setCurrentCity("上海");
        matched.setSchoolTier(SchoolTier.PROJECT_985.name());
        matched.setTotalYearsOfExperience(new BigDecimal("5.0"));
        matched.setOutsourcing(false);

        SearchHit<CandidateProfileIndex> candidateHit = org.mockito.Mockito.mock(SearchHit.class);
        when(candidateHit.getContent()).thenReturn(matched);
        when(candidateHit.getScore()).thenReturn(6.5f);

        SearchHits<CandidateProfileIndex> candidateHits = org.mockito.Mockito.mock(SearchHits.class);
        when(candidateHits.getSearchHits()).thenReturn(List.of(candidateHit));
        when(candidateHits.getTotalHits()).thenReturn(1L);

        SearchHits<ResumeChunk> resumeChunkHits = org.mockito.Mockito.mock(SearchHits.class);
        when(resumeChunkHits.getSearchHits()).thenReturn(List.of());

        when(elasticsearchOperations.search(any(Query.class), eq(CandidateProfileIndex.class))).thenReturn(candidateHits);
        when(elasticsearchOperations.search(any(Query.class), eq(ResumeChunk.class))).thenReturn(resumeChunkHits);

        CandidateSearchRequest request = new CandidateSearchRequest();
        request.setQuery("985 5年 上海 不要外包");

        CandidateSearchResponse response = service.search(request);

        assertEquals("", response.getQuery());
        assertEquals(1, response.getTotal());
        assertEquals("candidate-1", response.getCandidates().get(0).getCandidateId());
        verify(elasticsearchOperations).search(any(Query.class), eq(CandidateProfileIndex.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMergeVectorRecallCandidatesWhenKeywordSearchMisses() throws Exception {
        ElasticsearchOperations elasticsearchOperations = org.mockito.Mockito.mock(ElasticsearchOperations.class);
        com.recruit.agent.rag.embedding.EmbeddingService embeddingService = org.mockito.Mockito.mock(com.recruit.agent.rag.embedding.EmbeddingService.class);
        when(embeddingService.isAvailable()).thenReturn(true);
        when(embeddingService.embedAll(List.of("java 搜索工程师"))).thenReturn(List.of(new float[]{0.1f, 0.2f}));

        CandidateSearchServiceImpl service = new CandidateSearchServiceImpl(
            elasticsearchOperations,
            embeddingService,
            new SearchRequestNormalizationServiceImpl(new RuleBasedNaturalLanguageSearchFilterParser()),
            new DefaultCandidateMatchReasonService(),
            new DefaultCandidateSearchRerankService(new UnavailableRerankService())
        );

        SearchHits<CandidateProfileIndex> emptyKeywordHits = org.mockito.Mockito.mock(SearchHits.class);
        when(emptyKeywordHits.getSearchHits()).thenReturn(List.of());
        when(emptyKeywordHits.getTotalHits()).thenReturn(0L);

        ResumeChunk vectorChunk = new ResumeChunk();
        vectorChunk.setId("chunk-1");
        vectorChunk.setCandidateId("candidate-2");
        vectorChunk.setContent("负责 Java 搜索平台与召回优化");

        SearchHit<ResumeChunk> vectorChunkHit = org.mockito.Mockito.mock(SearchHit.class);
        when(vectorChunkHit.getContent()).thenReturn(vectorChunk);
        when(vectorChunkHit.getScore()).thenReturn(1.8f);

        SearchHits<ResumeChunk> vectorChunkHits = org.mockito.Mockito.mock(SearchHits.class);
        when(vectorChunkHits.getSearchHits()).thenReturn(List.of(vectorChunkHit));

        CandidateProfileIndex vectorCandidate = new CandidateProfileIndex();
        vectorCandidate.setCandidateId("candidate-2");
        vectorCandidate.setCandidateNo("C-002");
        vectorCandidate.setFullName("李四");
        vectorCandidate.setProfileSummary("有 Java 搜索相关经验");
        vectorCandidate.setTechnicalSkills(List.of("Java", "Elasticsearch"));

        SearchHit<CandidateProfileIndex> vectorCandidateHit = org.mockito.Mockito.mock(SearchHit.class);
        when(vectorCandidateHit.getContent()).thenReturn(vectorCandidate);
        when(vectorCandidateHit.getScore()).thenReturn(1.0f);

        SearchHits<CandidateProfileIndex> vectorCandidateHits = org.mockito.Mockito.mock(SearchHits.class);
        when(vectorCandidateHits.getSearchHits()).thenReturn(List.of(vectorCandidateHit));
        when(vectorCandidateHits.getTotalHits()).thenReturn(1L);

        SearchHits<ResumeChunk> evidenceHits = org.mockito.Mockito.mock(SearchHits.class);
        when(evidenceHits.getSearchHits()).thenReturn(List.of(vectorChunkHit));

        when(elasticsearchOperations.search(any(Query.class), eq(CandidateProfileIndex.class)))
            .thenReturn(emptyKeywordHits)
            .thenReturn(vectorCandidateHits);
        when(elasticsearchOperations.search(any(Query.class), eq(ResumeChunk.class)))
            .thenReturn(vectorChunkHits)
            .thenReturn(evidenceHits);

        CandidateSearchRequest request = new CandidateSearchRequest();
        request.setQuery("java 搜索工程师");
        request.setLimit(10);
        request.setEvidenceLimit(2);

        CandidateSearchResponse response = service.search(request);

        assertEquals(1, response.getTotal());
        assertEquals("candidate-2", response.getCandidates().get(0).getCandidateId());
        assertTrue(response.getCandidates().get(0).getMatchScore() > 0.0d);
        verify(embeddingService).embedAll(List.of("java 搜索工程师"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMergeProfileVectorRecallCandidatesWhenKeywordSearchMisses() throws Exception {
        ElasticsearchOperations elasticsearchOperations = org.mockito.Mockito.mock(ElasticsearchOperations.class);
        com.recruit.agent.rag.embedding.EmbeddingService embeddingService = org.mockito.Mockito.mock(com.recruit.agent.rag.embedding.EmbeddingService.class);
        when(embeddingService.isAvailable()).thenReturn(true);
        when(embeddingService.embedAll(List.of("java 搜索平台"))).thenReturn(List.of(new float[]{0.1f, 0.2f}));

        CandidateSearchServiceImpl service = new CandidateSearchServiceImpl(
            elasticsearchOperations,
            embeddingService,
            new SearchRequestNormalizationServiceImpl(new RuleBasedNaturalLanguageSearchFilterParser()),
            new DefaultCandidateMatchReasonService(),
            new DefaultCandidateSearchRerankService(new UnavailableRerankService())
        );

        SearchHits<CandidateProfileIndex> emptyKeywordHits = org.mockito.Mockito.mock(SearchHits.class);
        when(emptyKeywordHits.getSearchHits()).thenReturn(List.of());
        when(emptyKeywordHits.getTotalHits()).thenReturn(0L);

        CandidateProfileIndex profileVectorCandidate = new CandidateProfileIndex();
        profileVectorCandidate.setCandidateId("candidate-3");
        profileVectorCandidate.setCandidateNo("C-003");
        profileVectorCandidate.setFullName("王五");
        profileVectorCandidate.setProfileSummary("负责 Java 搜索平台与召回架构");
        profileVectorCandidate.setTechnicalSkills(List.of("Java", "Elasticsearch"));

        SearchHit<CandidateProfileIndex> profileVectorHit = org.mockito.Mockito.mock(SearchHit.class);
        when(profileVectorHit.getContent()).thenReturn(profileVectorCandidate);
        when(profileVectorHit.getScore()).thenReturn(1.9f);

        SearchHits<CandidateProfileIndex> profileVectorHits = org.mockito.Mockito.mock(SearchHits.class);
        when(profileVectorHits.getSearchHits()).thenReturn(List.of(profileVectorHit));
        when(profileVectorHits.getTotalHits()).thenReturn(1L);

        SearchHits<ResumeChunk> emptyChunkVectorHits = org.mockito.Mockito.mock(SearchHits.class);
        when(emptyChunkVectorHits.getSearchHits()).thenReturn(List.of());

        SearchHits<ResumeChunk> evidenceHits = org.mockito.Mockito.mock(SearchHits.class);
        when(evidenceHits.getSearchHits()).thenReturn(List.of());

        when(elasticsearchOperations.search(any(Query.class), eq(CandidateProfileIndex.class)))
            .thenReturn(emptyKeywordHits)
            .thenReturn(profileVectorHits);
        when(elasticsearchOperations.search(any(Query.class), eq(ResumeChunk.class)))
            .thenReturn(emptyChunkVectorHits)
            .thenReturn(evidenceHits);

        CandidateSearchRequest request = new CandidateSearchRequest();
        request.setQuery("java 搜索平台");
        request.setLimit(10);
        request.setEvidenceLimit(2);

        CandidateSearchResponse response = service.search(request);

        assertEquals(1, response.getTotal());
        assertEquals("candidate-3", response.getCandidates().get(0).getCandidateId());
        assertTrue(response.getCandidates().get(0).getMatchScore() > 0.0d);
        verify(embeddingService).embedAll(List.of("java 搜索平台"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFallbackWhenProfileVectorRecallFails() throws Exception {
        ElasticsearchOperations elasticsearchOperations = org.mockito.Mockito.mock(ElasticsearchOperations.class);
        com.recruit.agent.rag.embedding.EmbeddingService embeddingService = org.mockito.Mockito.mock(com.recruit.agent.rag.embedding.EmbeddingService.class);
        when(embeddingService.isAvailable()).thenReturn(true);
        when(embeddingService.embedAll(List.of("java search"))).thenReturn(List.of(new float[]{0.1f, 0.2f}));

        CandidateSearchServiceImpl service = new CandidateSearchServiceImpl(
            elasticsearchOperations,
            embeddingService,
            new SearchRequestNormalizationServiceImpl(new RuleBasedNaturalLanguageSearchFilterParser()),
            new DefaultCandidateMatchReasonService(),
            new DefaultCandidateSearchRerankService(new UnavailableRerankService())
        );

        CandidateProfileIndex keywordCandidate = new CandidateProfileIndex();
        keywordCandidate.setCandidateId("candidate-1");
        keywordCandidate.setCandidateNo("C-001");
        keywordCandidate.setFullName("Zhang San");
        keywordCandidate.setTechnicalSkills(List.of("Java"));

        SearchHit<CandidateProfileIndex> keywordHit = org.mockito.Mockito.mock(SearchHit.class);
        when(keywordHit.getContent()).thenReturn(keywordCandidate);
        when(keywordHit.getScore()).thenReturn(1.2f);

        SearchHits<CandidateProfileIndex> keywordHits = org.mockito.Mockito.mock(SearchHits.class);
        when(keywordHits.getSearchHits()).thenReturn(List.of(keywordHit));
        when(keywordHits.getTotalHits()).thenReturn(1L);

        SearchHits<ResumeChunk> emptyChunkHits = org.mockito.Mockito.mock(SearchHits.class);
        when(emptyChunkHits.getSearchHits()).thenReturn(List.of());

        SearchHits<ResumeChunk> evidenceHits = org.mockito.Mockito.mock(SearchHits.class);
        when(evidenceHits.getSearchHits()).thenReturn(List.of());

        when(elasticsearchOperations.search(any(Query.class), eq(CandidateProfileIndex.class)))
            .thenReturn(keywordHits)
            .thenThrow(new RuntimeException("profile vector failed"));
        when(elasticsearchOperations.search(any(Query.class), eq(ResumeChunk.class)))
            .thenReturn(emptyChunkHits)
            .thenReturn(evidenceHits);

        CandidateSearchRequest request = new CandidateSearchRequest();
        request.setQuery("java search");
        request.setLimit(10);
        request.setEvidenceLimit(1);

        CandidateSearchResponse response = service.search(request);

        assertEquals(1, response.getTotal());
        assertEquals("candidate-1", response.getCandidates().get(0).getCandidateId());
    }
}
