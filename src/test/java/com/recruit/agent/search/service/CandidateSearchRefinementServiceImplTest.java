package com.recruit.agent.search.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruit.agent.candidate.model.CareerStage;
import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import com.recruit.agent.search.dto.CandidateSearchRefineRequest;
import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.dto.FilterMergeMode;
import com.recruit.agent.search.parser.SearchIntentParseResult;
import com.recruit.agent.search.parser.SearchIntentParser;
import com.recruit.agent.search.parser.impl.RuleBasedNaturalLanguageSearchFilterParser;
import com.recruit.agent.search.service.impl.CandidateSearchRefinementServiceImpl;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CandidateSearchRefinementServiceImplTest {

    @Test
    void shouldAppendQueryAndMergeFiltersForRefinement() {
        CandidateSearchService candidateSearchService = org.mockito.Mockito.mock(CandidateSearchService.class);
        CandidateSearchRefinementServiceImpl service = new CandidateSearchRefinementServiceImpl(
            candidateSearchService,
            new RuleBasedNaturalLanguageSearchFilterParser()
        );

        CandidateSearchFilter baseFilter = new CandidateSearchFilter();
        baseFilter.setHighestDegrees(List.of(DegreeLevel.BACHELOR));
        baseFilter.setTechnicalSkills(List.of("Java"));
        baseFilter.setMinYearsOfExperience(new BigDecimal("3.0"));
        baseFilter.setBigTech(true);

        CandidateSearchRequest baseRequest = new CandidateSearchRequest();
        baseRequest.setQuery("推荐系统");
        baseRequest.setFilter(baseFilter);
        baseRequest.setScopeCandidateIds(List.of("candidate-1", "candidate-2"));

        CandidateSearchFilter refinementFilter = new CandidateSearchFilter();
        refinementFilter.setCareerStage(CareerStage.EARLY_CAREER);
        refinementFilter.setSchoolTiers(List.of(SchoolTier.PROJECT_985));
        refinementFilter.setTechnicalSkills(List.of("Elasticsearch"));
        refinementFilter.setMinYearsOfExperience(new BigDecimal("5.0"));

        CandidateSearchRefineRequest refineRequest = new CandidateSearchRefineRequest();
        refineRequest.setBaseRequest(baseRequest);
        refineRequest.setRefinementQuery("校招 Java 985 5年 上海");
        refineRequest.setRefinementFilter(refinementFilter);
        refineRequest.setMergeMode(FilterMergeMode.APPEND);

        CandidateSearchRequest merged = service.merge(refineRequest);

        assertEquals("推荐系统 java", merged.getQuery());
        assertEquals(CareerStage.EARLY_CAREER, merged.getFilter().getCareerStage());
        assertIterableEquals(List.of(DegreeLevel.BACHELOR), merged.getFilter().getHighestDegrees());
        assertIterableEquals(List.of(SchoolTier.PROJECT_985), merged.getFilter().getSchoolTiers());
        assertIterableEquals(List.of("Java", "Elasticsearch"), merged.getFilter().getTechnicalSkills());
        assertEquals(new BigDecimal("5.0"), merged.getFilter().getMinYearsOfExperience());
        assertEquals("上海", merged.getFilter().getCurrentCity());
        assertIterableEquals(List.of("candidate-1", "candidate-2"), merged.getScopeCandidateIds());
    }

    @Test
    void shouldReplaceQueryFilterAndScopeDuringRefinementSearch() {
        CandidateSearchService candidateSearchService = org.mockito.Mockito.mock(CandidateSearchService.class);
        CandidateSearchRefinementServiceImpl service = new CandidateSearchRefinementServiceImpl(
            candidateSearchService,
            new RuleBasedNaturalLanguageSearchFilterParser()
        );

        CandidateSearchRequest baseRequest = new CandidateSearchRequest();
        baseRequest.setQuery("推荐系统");
        baseRequest.setScopeCandidateIds(List.of("candidate-1"));

        CandidateSearchFilter refinementFilter = new CandidateSearchFilter();
        refinementFilter.setCareerStage(CareerStage.EARLY_CAREER);
        refinementFilter.setHighestDegrees(List.of(DegreeLevel.MASTER));
        refinementFilter.setCurrentCity("上海");

        CandidateSearchRefineRequest refineRequest = new CandidateSearchRefineRequest();
        refineRequest.setBaseRequest(baseRequest);
        refineRequest.setRefinementQuery("搜索");
        refineRequest.setRefinementFilter(refinementFilter);
        refineRequest.setMergeMode(FilterMergeMode.REPLACE);
        refineRequest.setScopeCandidateIds(List.of("candidate-3"));

        CandidateSearchResponse expected = new CandidateSearchResponse();
        when(candidateSearchService.search(any(CandidateSearchRequest.class))).thenReturn(expected);

        CandidateSearchResponse actual = service.refineSearch(refineRequest);

        assertEquals(expected, actual);
        verify(candidateSearchService).search(org.mockito.ArgumentMatchers.argThat(request ->
            "搜索".equals(request.getQuery())
                && request.getFilter() != null
                && request.getFilter().getCareerStage() == CareerStage.EARLY_CAREER
                && List.of(DegreeLevel.MASTER).equals(request.getFilter().getHighestDegrees())
                && "上海".equals(request.getFilter().getCurrentCity())
                && List.of("candidate-3").equals(request.getScopeCandidateIds())
        ));
    }

    @Test
    void shouldUseLlmResidualQueryDuringRefinementWhenAvailable() {
        CandidateSearchService candidateSearchService = org.mockito.Mockito.mock(CandidateSearchService.class);
        SearchIntentParseResult llmResult = new SearchIntentParseResult();
        CandidateSearchFilter llmFilter = new CandidateSearchFilter();
        llmFilter.setCareerStage(CareerStage.EARLY_CAREER);
        llmFilter.setCurrentCity("上海");
        llmFilter.setTechnicalSkills(List.of("Elasticsearch"));
        llmResult.setResidualQuery("推荐系统");
        llmResult.setFilter(llmFilter);

        CandidateSearchRefinementServiceImpl service = new CandidateSearchRefinementServiceImpl(
            candidateSearchService,
            new RuleBasedNaturalLanguageSearchFilterParser(),
            new StubSearchIntentParser(true, llmResult)
        );

        CandidateSearchRequest baseRequest = new CandidateSearchRequest();
        baseRequest.setQuery("Java");

        CandidateSearchRefineRequest refineRequest = new CandidateSearchRefineRequest();
        refineRequest.setBaseRequest(baseRequest);
        refineRequest.setRefinementQuery("校招 上海 Elasticsearch 推荐系统");
        refineRequest.setMergeMode(FilterMergeMode.APPEND);

        CandidateSearchRequest merged = service.merge(refineRequest);

        assertEquals("Java elasticsearch 推荐系统", merged.getQuery());
        assertEquals(CareerStage.EARLY_CAREER, merged.getFilter().getCareerStage());
        assertIterableEquals(List.of("Elasticsearch"), merged.getFilter().getTechnicalSkills());
        assertEquals("上海", merged.getFilter().getCurrentCity());
    }

    private static class StubSearchIntentParser implements SearchIntentParser {
        private final boolean available;
        private final SearchIntentParseResult result;

        private StubSearchIntentParser(boolean available, SearchIntentParseResult result) {
            this.available = available;
            this.result = result;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public SearchIntentParseResult parse(String text) {
            return result;
        }
    }
}
