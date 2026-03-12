package com.recruit.agent.agent.tool.springai;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruit.agent.search.dto.CandidateSearchRefineRequest;
import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.service.CandidateSearchRefinementService;
import com.recruit.agent.search.service.CandidateSearchService;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import org.junit.jupiter.api.Test;

class SpringAiToolsDelegationTest {

    @Test
    void shouldDelegateSearchToolAnnotationBean() {
        CandidateSearchService searchService = org.mockito.Mockito.mock(CandidateSearchService.class);
        SearchCandidateTools tools = new SearchCandidateTools(searchService);
        CandidateSearchResponse expected = new CandidateSearchResponse();
        when(searchService.search(any(CandidateSearchRequest.class))).thenReturn(expected);

        CandidateSearchResponse actual = tools.searchCandidateByJDTool(new CandidateSearchRequest());

        assertSame(expected, actual);
        verify(searchService).search(any(CandidateSearchRequest.class));
    }

    @Test
    void shouldDelegateRefineToolAnnotationBean() {
        CandidateSearchRefinementService refinementService = org.mockito.Mockito.mock(CandidateSearchRefinementService.class);
        RefineSearchTools tools = new RefineSearchTools(refinementService);
        CandidateSearchResponse expected = new CandidateSearchResponse();
        when(refinementService.refineSearch(any(CandidateSearchRefineRequest.class))).thenReturn(expected);

        CandidateSearchResponse actual = tools.refineSearchFilterTool(new CandidateSearchRefineRequest());

        assertSame(expected, actual);
        verify(refinementService).refineSearch(any(CandidateSearchRefineRequest.class));
    }
}
