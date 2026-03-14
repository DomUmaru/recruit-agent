package com.recruit.agent.search.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.recruit.agent.candidate.model.CareerStage;
import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import com.recruit.agent.search.parser.impl.RuleBasedNaturalLanguageSearchFilterParser;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleBasedNaturalLanguageSearchFilterParserTest {

    private final RuleBasedNaturalLanguageSearchFilterParser parser = new RuleBasedNaturalLanguageSearchFilterParser();

    @Test
    void shouldParseChineseStructuredFilters() {
        CandidateSearchFilter filter = parser.parse("上海 985 硕士 5年 Java Elasticsearch 不要外包");

        assertEquals(null, filter.getCareerStage());
        assertIterableEquals(List.of(DegreeLevel.MASTER), filter.getHighestDegrees());
        assertIterableEquals(List.of(SchoolTier.PROJECT_985), filter.getSchoolTiers());
        assertEquals(new BigDecimal("5"), filter.getMinYearsOfExperience());
        assertIterableEquals(List.of("Java", "Elasticsearch"), filter.getTechnicalSkills());
        assertEquals("上海", filter.getCurrentCity());
        assertEquals(Boolean.FALSE, filter.getOutsourcing());
    }

    @Test
    void shouldParseEarlyCareerIntent() {
        CandidateSearchFilter filter = parser.parse("校招 应届 Java 后端");

        assertEquals(CareerStage.EARLY_CAREER, filter.getCareerStage());
        assertIterableEquals(List.of("Java"), filter.getTechnicalSkills());
    }

    @Test
    void shouldNotInferBachelorFrom985Or211() {
        CandidateSearchFilter filter = parser.parse("985 211 Java");

        assertEquals(null, filter.getHighestDegrees());
        assertIterableEquals(List.of(SchoolTier.PROJECT_985, SchoolTier.PROJECT_211), filter.getSchoolTiers());
        assertIterableEquals(List.of("Java"), filter.getTechnicalSkills());
    }

    @Test
    void shouldStripCareerStageAndStructuredTermsFromResidualQuery() {
        String residual = parser.stripFilterTerms("校招 上海 985 硕士 5年 Java Elasticsearch 推荐系统 不要外包");

        assertEquals("java elasticsearch 推荐系统", residual);
    }
}
