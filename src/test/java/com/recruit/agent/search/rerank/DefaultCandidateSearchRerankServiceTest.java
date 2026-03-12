package com.recruit.agent.search.rerank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruit.agent.search.rerank.impl.DefaultCandidateSearchRerankService;
import com.recruit.agent.search.vo.CandidateSearchEvidenceVO;
import com.recruit.agent.search.vo.CandidateSearchItemVO;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

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

    private CandidateSearchItemVO candidate(String candidateId, double matchScore, String summary, String evidenceContent) {
        CandidateSearchItemVO item = new CandidateSearchItemVO();
        item.setCandidateId(candidateId);
        item.setMatchScore(matchScore);
        item.setProfileSummary(summary);

        CandidateSearchEvidenceVO evidence = new CandidateSearchEvidenceVO();
        evidence.setContent(evidenceContent);
        item.setEvidenceList(List.of(evidence));
        return item;
    }
}
