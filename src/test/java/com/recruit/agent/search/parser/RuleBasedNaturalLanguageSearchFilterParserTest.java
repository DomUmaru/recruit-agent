package com.recruit.agent.search.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import com.recruit.agent.search.parser.impl.RuleBasedNaturalLanguageSearchFilterParser;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RuleBasedNaturalLanguageSearchFilterParserTest {

    @Test
    void shouldParseStructuredFilterFromNaturalLanguage() {
        RuleBasedNaturalLanguageSearchFilterParser parser = new RuleBasedNaturalLanguageSearchFilterParser();

        CandidateSearchFilter filter = parser.parse("只要985和211，本科，3年经验，上海，Java 和 Elasticsearch，不要外包，优先大厂");

        assertTrue(filter.getHighestDegrees().contains(DegreeLevel.BACHELOR));
        assertTrue(filter.getSchoolTiers().contains(SchoolTier.PROJECT_985));
        assertTrue(filter.getSchoolTiers().contains(SchoolTier.PROJECT_211));
        assertEquals(new BigDecimal("3"), filter.getMinYearsOfExperience());
        assertEquals("上海", filter.getCurrentCity());
        assertTrue(filter.getTechnicalSkills().contains("Java"));
        assertTrue(filter.getTechnicalSkills().contains("Elasticsearch"));
        assertEquals(Boolean.TRUE, filter.getBigTech());
        assertEquals(Boolean.FALSE, filter.getOutsourcing());
    }

    @Test
    void shouldReturnEmptyFilterWhenNothingCanBeParsed() {
        RuleBasedNaturalLanguageSearchFilterParser parser = new RuleBasedNaturalLanguageSearchFilterParser();

        CandidateSearchFilter filter = parser.parse("帮我找推荐系统候选人");

        assertEquals(null, filter.getHighestDegrees());
        assertEquals(null, filter.getSchoolTiers());
        assertEquals(null, filter.getMinYearsOfExperience());
        assertFalse(filter.getTechnicalSkills() != null && !filter.getTechnicalSkills().isEmpty());
    }

    @Test
    void shouldStripRecognizedFilterTermsAndKeepResidualQuery() {
        RuleBasedNaturalLanguageSearchFilterParser parser = new RuleBasedNaturalLanguageSearchFilterParser();

        String residual = parser.stripFilterTerms("推荐系统 Java 985 5年 上海 不要外包");

        assertEquals("推荐系统 java", residual);
    }
}
