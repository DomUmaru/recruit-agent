package com.recruit.agent.search.rerank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        CandidateSearchItemVO first = candidate("candidate-1", 8.5, "Java 开发", "普通 CRUD");
        CandidateSearchItemVO second = candidate("candidate-2", 7.5, "搜索工程师", "搜索召回与排序");

        List<CandidateSearchItemVO> reranked = service.rerank("java search", List.of(first, second));

        assertEquals(List.of("candidate-2", "candidate-1"), reranked.stream().map(CandidateSearchItemVO::getCandidateId).toList());
        assertEquals(0.9, reranked.get(0).getRerankScore());
        assertEquals(0.2, reranked.get(1).getRerankScore());
    }

    @Test
    void shouldSkipRerankWhenProviderUnavailable() throws IOException {
        RerankService rerankService = org.mockito.Mockito.mock(RerankService.class);
        DefaultCandidateSearchRerankService service = new DefaultCandidateSearchRerankService(rerankService);

        when(rerankService.isAvailable()).thenReturn(false);

        CandidateSearchItemVO first = candidate("candidate-1", 8.5, "Java 开发", "普通 CRUD");
        List<CandidateSearchItemVO> reranked = service.rerank("java search", List.of(first));

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

        List<CandidateSearchItemVO> reranked = service.rerank("java search", candidates);

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

        service.rerank("java search", List.of(candidate));

        ArgumentCaptor<List<String>> documentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(rerankService).rerank(eq("java search"), documentsCaptor.capture());
        String document = documentsCaptor.getValue().get(0);
        assertTrue(document.contains("name: 张三"));
        assertTrue(document.contains("city: 上海"));
        assertTrue(document.contains("skills: java, elasticsearch"));
        assertTrue(document.contains("evidence: 负责召回与排序") || document.contains("evidence_"));
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
