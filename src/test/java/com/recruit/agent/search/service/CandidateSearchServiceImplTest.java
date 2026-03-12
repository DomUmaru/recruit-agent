package com.recruit.agent.search.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import com.recruit.agent.rag.model.CandidateProfileIndex;
import com.recruit.agent.rag.model.ResumeChunk;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import com.recruit.agent.search.dto.CandidateSearchRequest;
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
        CandidateSearchServiceImpl service = new CandidateSearchServiceImpl(elasticsearchOperations);

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
        matched.setProfileSummary("具备推荐系统和搜索相关经验，熟悉 Java 与 Elasticsearch。");

        CandidateProfileIndex filteredOut = new CandidateProfileIndex();
        filteredOut.setId("idx-2");
        filteredOut.setCandidateId("candidate-2");
        filteredOut.setCandidateNo("C-002");
        filteredOut.setFullName("李四");
        filteredOut.setHighestDegree(DegreeLevel.ASSOCIATE.name());
        filteredOut.setSchoolTier(SchoolTier.OTHER.name());
        filteredOut.setTotalYearsOfExperience(new BigDecimal("2.0"));
        filteredOut.setTechnicalSkills(List.of("PHP"));
        filteredOut.setBigTech(false);
        filteredOut.setOutsourcing(true);
        filteredOut.setProfileSummary("主要从事 PHP 项目开发。");

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
        assertFalse(response.getCandidates().get(0).getEvidenceList().isEmpty());
        assertFalse(response.getCandidates().get(0).getMatchReasons().isEmpty());
    }
}
