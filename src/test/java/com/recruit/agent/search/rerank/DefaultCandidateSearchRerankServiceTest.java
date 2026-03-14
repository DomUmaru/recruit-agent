package com.recruit.agent.search.rerank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruit.agent.position.model.PositionJD;
import com.recruit.agent.search.rerank.impl.DefaultCandidateSearchRerankService;
import com.recruit.agent.search.vo.CandidateSearchEvidenceVO;
import com.recruit.agent.search.vo.CandidateSearchItemVO;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DefaultCandidateSearchRerankServiceTest {

    @Test
    void shouldReorderCandidatesByRerankScore() throws IOException {
        RerankService rerankService = org.mockito.Mockito.mock(RerankService.class);
        DefaultCandidateSearchRerankService service = new DefaultCandidateSearchRerankService(rerankService);

        when(rerankService.isAvailable()).thenReturn(true);
        when(rerankService.rerank(eq("java search"), anyList())).thenReturn(List.of(0.2, 0.9));

        CandidateSearchItemVO first = candidate("candidate-1", 8.5, "java backend", "crud service");
        CandidateSearchItemVO second = candidate("candidate-2", 7.5, "search backend", "recall and ranking");

        List<CandidateSearchItemVO> reranked = service.rerank("java search", List.of(first, second), null);

        assertEquals(List.of("candidate-2", "candidate-1"), reranked.stream().map(CandidateSearchItemVO::getCandidateId).toList());
        assertEquals(0.9, reranked.get(0).getRerankScore());
        assertEquals(0.2, reranked.get(1).getRerankScore());
        assertTrue(reranked.get(0).getFinalScore() >= reranked.get(1).getFinalScore());
    }

    @Test
    void shouldSkipRerankWhenProviderUnavailable() throws IOException {
        RerankService rerankService = org.mockito.Mockito.mock(RerankService.class);
        DefaultCandidateSearchRerankService service = new DefaultCandidateSearchRerankService(rerankService);

        when(rerankService.isAvailable()).thenReturn(false);

        CandidateSearchItemVO first = candidate("candidate-1", 8.5, "java backend", "crud service");
        List<CandidateSearchItemVO> reranked = service.rerank("java search", List.of(first), null);

        assertEquals(List.of("candidate-1"), reranked.stream().map(CandidateSearchItemVO::getCandidateId).toList());
        verify(rerankService, never()).rerank(eq("java search"), anyList());
    }

    @Test
    void shouldOnlyRerankTopWindowAndKeepTailOrder() throws IOException {
        RerankService rerankService = org.mockito.Mockito.mock(RerankService.class);
        DefaultCandidateSearchRerankService service = new DefaultCandidateSearchRerankService(rerankService);

        when(rerankService.isAvailable()).thenReturn(true);
        when(rerankService.rerank(eq("java search"), anyList()))
            .thenReturn(java.util.Collections.nCopies(20, 0.5d));

        List<CandidateSearchItemVO> candidates = new ArrayList<>();
        for (int index = 1; index <= 22; index++) {
            candidates.add(candidate("candidate-" + index, 30 - index, "summary-" + index, "evidence-" + index));
        }

        List<CandidateSearchItemVO> reranked = service.rerank("java search", candidates, null);

        ArgumentCaptor<List<String>> documentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(rerankService).rerank(eq("java search"), documentsCaptor.capture());
        assertEquals(20, documentsCaptor.getValue().size());
        assertEquals("candidate-21", reranked.get(20).getCandidateId());
        assertEquals("candidate-22", reranked.get(21).getCandidateId());
    }

    @Test
    void shouldBuildStructuredRerankDocument() throws IOException {
        RerankService rerankService = org.mockito.Mockito.mock(RerankService.class);
        DefaultCandidateSearchRerankService service = new DefaultCandidateSearchRerankService(rerankService);

        when(rerankService.isAvailable()).thenReturn(true);
        when(rerankService.rerank(eq("java search"), anyList())).thenReturn(List.of(0.9));

        CandidateSearchItemVO candidate = candidate("candidate-1", 8.5, "搜索平台经验", "负责召回与排序");
        candidate.setFullName("张三");
        candidate.setCurrentCity("上海");
        candidate.setHighestDegree("BACHELOR");
        candidate.setSchoolTier("PROJECT_985");
        candidate.setTotalYearsOfExperience(new BigDecimal("5.0"));
        candidate.setTechnicalSkills(List.of("Java", "Elasticsearch"));
        candidate.setBigTech(true);
        candidate.setOutsourcing(false);

        service.rerank("java search", List.of(candidate), null);

        ArgumentCaptor<List<String>> documentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(rerankService).rerank(eq("java search"), documentsCaptor.capture());
        String document = documentsCaptor.getValue().get(0);
        assertTrue(document.contains("name: 张三".toLowerCase()));
        assertTrue(document.contains("city: 上海".toLowerCase()));
        assertTrue(document.contains("skills: java, elasticsearch"));
        assertTrue(document.contains("evidence"));
    }

    @Test
    void shouldUseWeakPreferenceScoreAsSecondarySignal() throws IOException {
        RerankService rerankService = org.mockito.Mockito.mock(RerankService.class);
        DefaultCandidateSearchRerankService service = new DefaultCandidateSearchRerankService(rerankService);

        when(rerankService.isAvailable()).thenReturn(true);
        when(rerankService.rerank(eq("java backend"), anyList())).thenReturn(List.of(0.60, 0.58));

        CandidateSearchItemVO stronger = candidate("candidate-stronger", 8.0, "java backend", "spring boot");
        stronger.setHighestDegree("MASTER");
        stronger.setSchoolTier("PROJECT_985");
        stronger.setTotalYearsOfExperience(new BigDecimal("5"));
        stronger.setBigTech(true);
        stronger.setOutsourcing(false);

        CandidateSearchItemVO weaker = candidate("candidate-weaker", 8.0, "java backend", "spring boot");
        weaker.setHighestDegree("BACHELOR");
        weaker.setSchoolTier("GENERAL_UNDERGRAD");
        weaker.setTotalYearsOfExperience(new BigDecimal("1"));
        weaker.setBigTech(false);
        weaker.setOutsourcing(true);

        List<CandidateSearchItemVO> reranked = service.rerank("java backend", List.of(weaker, stronger), null);

        assertEquals("candidate-stronger", reranked.get(0).getCandidateId());
        assertTrue(reranked.get(0).getPreferenceScore() > reranked.get(1).getPreferenceScore());
        assertTrue(reranked.get(0).getFinalScore() > reranked.get(1).getFinalScore());
    }

    @Test
    void shouldDisableExperiencePreferenceForEarlyCareerIntent() throws IOException {
        RerankService rerankService = org.mockito.Mockito.mock(RerankService.class);
        DefaultCandidateSearchRerankService service = new DefaultCandidateSearchRerankService(rerankService);

        when(rerankService.isAvailable()).thenReturn(true);
        when(rerankService.rerank(eq("校招 Java 后端"), anyList())).thenReturn(List.of(0.60, 0.58));

        CandidateSearchItemVO experienced = candidate("candidate-experienced", 8.0, "java backend", "spring boot");
        experienced.setHighestDegree("BACHELOR");
        experienced.setSchoolTier("GENERAL_UNDERGRAD");
        experienced.setTotalYearsOfExperience(new BigDecimal("6"));
        experienced.setBigTech(false);
        experienced.setOutsourcing(false);

        CandidateSearchItemVO student = candidate("candidate-student", 8.0, "java backend", "intern project");
        student.setHighestDegree("BACHELOR");
        student.setSchoolTier("GENERAL_UNDERGRAD");
        student.setTotalYearsOfExperience(new BigDecimal("0"));
        student.setBigTech(false);
        student.setOutsourcing(false);

        List<CandidateSearchItemVO> reranked = service.rerank("校招 Java 后端", List.of(experienced, student), null);

        assertEquals(0.0d, reranked.get(0).getPreferenceScore() - reranked.get(1).getPreferenceScore(), 0.000001d);
        assertEquals("candidate-experienced", reranked.get(0).getCandidateId());
    }

    @Test
    void shouldUseJdPreferenceAsAdditionalSignal() throws IOException {
        RerankService rerankService = org.mockito.Mockito.mock(RerankService.class);
        DefaultCandidateSearchRerankService service = new DefaultCandidateSearchRerankService(rerankService);

        when(rerankService.isAvailable()).thenReturn(true);
        when(rerankService.rerank(eq("java backend"), anyList())).thenReturn(List.of(0.60, 0.60));

        PositionJD positionJD = new PositionJD();
        positionJD.setTitle("搜索推荐 Java 后端工程师");
        positionJD.setPrioritySkills("Java, Spring Boot, Elasticsearch");
        positionJD.setBonusSkills("推荐系统, Kafka");

        CandidateSearchItemVO stronger = candidate("candidate-stronger", 8.0, "搜索推荐后端", "负责推荐系统和 Elasticsearch 检索服务");
        stronger.setTechnicalSkills(List.of("Java", "Spring Boot", "Elasticsearch", "Kafka"));

        CandidateSearchItemVO weaker = candidate("candidate-weaker", 8.0, "普通后端", "负责通用 CRUD 服务");
        weaker.setTechnicalSkills(List.of("Java", "Spring MVC"));

        List<CandidateSearchItemVO> reranked = service.rerank("java backend", List.of(weaker, stronger), positionJD);

        assertEquals("candidate-stronger", reranked.get(0).getCandidateId());
        assertTrue(reranked.get(0).getJdPreferenceScore() > reranked.get(1).getJdPreferenceScore());
        assertTrue(reranked.get(0).getFinalScore() > reranked.get(1).getFinalScore());
    }

    private CandidateSearchItemVO candidate(String candidateId, double matchScore, String summary, String evidenceContent) {
        CandidateSearchItemVO item = new CandidateSearchItemVO();
        item.setCandidateId(candidateId);
        item.setCandidateNo(candidateId);
        item.setMatchScore(matchScore);
        item.setProfileSummary(summary);

        CandidateSearchEvidenceVO evidence = new CandidateSearchEvidenceVO();
        evidence.setContent(evidenceContent);
        item.setEvidenceList(List.of(evidence));
        return item;
    }
}
