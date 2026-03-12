package com.recruit.agent.agent.tool;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruit.agent.agent.tool.impl.RefineSearchFilterToolServiceImpl;
import com.recruit.agent.agent.tool.impl.SearchCandidateToolServiceImpl;
import com.recruit.agent.search.dto.CandidateSearchRefineRequest;
import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.service.CandidateSearchRefinementService;
import com.recruit.agent.search.service.CandidateSearchService;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import org.junit.jupiter.api.Test;

class SearchToolsDelegationTest {

    @Test
    void shouldDelegateSearchToolToSearchService() {
        CandidateSearchService searchService = org.mockito.Mockito.mock(CandidateSearchService.class);
        SearchCandidateToolServiceImpl toolService = new SearchCandidateToolServiceImpl(searchService);
        CandidateSearchResponse expected = new CandidateSearchResponse();
        when(searchService.search(any(CandidateSearchRequest.class))).thenReturn(expected);

        CandidateSearchResponse actual = toolService.execute(new CandidateSearchRequest());

        assertSame(expected, actual);
        verify(searchService).search(any(CandidateSearchRequest.class));
    }

    @Test
    void shouldDelegateRefineToolToRefinementService() {
        CandidateSearchRefinementService refinementService = org.mockito.Mockito.mock(CandidateSearchRefinementService.class);
        RefineSearchFilterToolServiceImpl toolService = new RefineSearchFilterToolServiceImpl(refinementService);
        CandidateSearchResponse expected = new CandidateSearchResponse();
        when(refinementService.refineSearch(any(CandidateSearchRefineRequest.class))).thenReturn(expected);

        CandidateSearchResponse actual = toolService.execute(new CandidateSearchRefineRequest());

        assertSame(expected, actual);
        verify(refinementService).refineSearch(any(CandidateSearchRefineRequest.class));
    }
}
