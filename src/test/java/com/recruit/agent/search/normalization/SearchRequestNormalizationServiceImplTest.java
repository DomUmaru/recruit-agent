package com.recruit.agent.search.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.normalization.impl.SearchRequestNormalizationServiceImpl;
import com.recruit.agent.search.parser.impl.RuleBasedNaturalLanguageSearchFilterParser;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchRequestNormalizationServiceImplTest {

    @Test
    void shouldPrepareEffectiveQueryAndMergeParsedFilter() {
        SearchRequestNormalizationServiceImpl service = new SearchRequestNormalizationServiceImpl(
            new RuleBasedNaturalLanguageSearchFilterParser()
        );

        CandidateSearchFilter explicitFilter = new CandidateSearchFilter();
        explicitFilter.setHighestDegrees(List.of(DegreeLevel.BACHELOR));
        explicitFilter.setTechnicalSkills(List.of("Java"));
        explicitFilter.setBigTech(true);

        CandidateSearchRequest request = new CandidateSearchRequest();
        request.setQuery("推荐系统 Java 985 5年 上海 不要外包");
        request.setFilter(explicitFilter);
        request.setScopeCandidateIds(List.of("candidate-1", "candidate-2"));

        PreparedCandidateSearchRequest prepared = service.prepare(request);

        assertEquals("推荐系统 java", prepared.getQuery());
        assertEquals("推荐系统 Java 985 5年 上海 不要外包", prepared.getRawQuery());
        assertIterableEquals(List.of(DegreeLevel.BACHELOR), prepared.getFilter().getHighestDegrees());
        assertIterableEquals(List.of(SchoolTier.PROJECT_985), prepared.getFilter().getSchoolTiers());
        assertIterableEquals(List.of("Java"), prepared.getFilter().getTechnicalSkills());
        assertEquals(new BigDecimal("5"), prepared.getFilter().getMinYearsOfExperience());
        assertEquals("上海", prepared.getFilter().getCurrentCity());
        assertEquals(Boolean.TRUE, prepared.getFilter().getBigTech());
        assertEquals(Boolean.FALSE, prepared.getFilter().getOutsourcing());
        assertIterableEquals(List.of("candidate-1", "candidate-2"), prepared.getScopeCandidateIds());
        assertEquals(10, prepared.getLimit());
        assertEquals(3, prepared.getEvidenceLimit());
    }

    @Test
    void shouldKeepExplicitOverridesWhenParsedFilterConflicts() {
        SearchRequestNormalizationServiceImpl service = new SearchRequestNormalizationServiceImpl(
            new RuleBasedNaturalLanguageSearchFilterParser()
        );

        CandidateSearchFilter explicitFilter = new CandidateSearchFilter();
        explicitFilter.setCurrentCity("北京");
        explicitFilter.setOutsourcing(false);

        CandidateSearchRequest request = new CandidateSearchRequest();
        request.setQuery("上海 外包 Java");
        request.setFilter(explicitFilter);

        PreparedCandidateSearchRequest prepared = service.prepare(request);

        assertEquals("java", prepared.getQuery());
        assertEquals("北京", prepared.getFilter().getCurrentCity());
        assertEquals(Boolean.FALSE, prepared.getFilter().getOutsourcing());
        assertIterableEquals(List.of("Java"), prepared.getFilter().getTechnicalSkills());
    }

    @Test
    void shouldNormalizeLimitsAndAllowFilterOnlyQuery() {
        SearchRequestNormalizationServiceImpl service = new SearchRequestNormalizationServiceImpl(
            new RuleBasedNaturalLanguageSearchFilterParser()
        );

        CandidateSearchRequest request = new CandidateSearchRequest();
        request.setQuery("985 5年 上海 不要外包");
        request.setLimit(0);
        request.setEvidenceLimit(-1);

        PreparedCandidateSearchRequest prepared = service.prepare(request);

        assertEquals("", prepared.getQuery());
        assertIterableEquals(List.of(DegreeLevel.BACHELOR), prepared.getFilter().getHighestDegrees());
        assertIterableEquals(List.of(SchoolTier.PROJECT_985), prepared.getFilter().getSchoolTiers());
        assertEquals(new BigDecimal("5"), prepared.getFilter().getMinYearsOfExperience());
        assertEquals("上海", prepared.getFilter().getCurrentCity());
        assertEquals(Boolean.FALSE, prepared.getFilter().getOutsourcing());
        assertEquals(10, prepared.getLimit());
        assertEquals(3, prepared.getEvidenceLimit());
    }
}
