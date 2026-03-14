package com.recruit.agent.search.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.candidate.model.CareerStage;
import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import com.recruit.agent.llm.LlmGenerationService;
import com.recruit.agent.search.parser.impl.LlmSearchIntentParser;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class LlmSearchIntentParserTest {

    @Test
    void shouldParseResidualQueryAndStructuredFilter() {
        LlmSearchIntentParser parser = new LlmSearchIntentParser(
            new StubLlmGenerationService(true, """
                {"residualQuery":"推荐系统","careerStage":"EARLY_CAREER","highestDegrees":["MASTER"],"schoolTiers":["PROJECT_985"],"minYearsOfExperience":5,"technicalSkills":["Java","Elasticsearch"],"currentCity":"上海","bigTech":true,"outsourcing":false}
                """),
            new ObjectMapper()
        );

        SearchIntentParseResult result = parser.parse("上海 985 硕士 5年 Java Elasticsearch 推荐系统");

        assertEquals("推荐系统", result.getResidualQuery());
        assertEquals(CareerStage.EARLY_CAREER, result.getFilter().getCareerStage());
        assertIterableEquals(List.of(DegreeLevel.MASTER), result.getFilter().getHighestDegrees());
        assertIterableEquals(List.of(SchoolTier.PROJECT_985), result.getFilter().getSchoolTiers());
        assertEquals(new BigDecimal("5"), result.getFilter().getMinYearsOfExperience());
        assertIterableEquals(List.of("Java", "Elasticsearch"), result.getFilter().getTechnicalSkills());
        assertEquals("上海", result.getFilter().getCurrentCity());
        assertEquals(Boolean.TRUE, result.getFilter().getBigTech());
        assertEquals(Boolean.FALSE, result.getFilter().getOutsourcing());
    }

    @Test
    void shouldReturnEmptyResultWhenUnavailable() {
        LlmSearchIntentParser parser = new LlmSearchIntentParser(
            new StubLlmGenerationService(false, ""),
            new ObjectMapper()
        );

        SearchIntentParseResult result = parser.parse("Java 推荐系统");

        assertEquals(null, result.getResidualQuery());
        assertEquals(null, result.getFilter().getCareerStage());
        assertEquals(null, result.getFilter().getHighestDegrees());
        assertEquals(null, result.getFilter().getTechnicalSkills());
    }

    @Test
    void shouldParseJsonWrappedInMarkdownFence() {
        LlmSearchIntentParser parser = new LlmSearchIntentParser(
            new StubLlmGenerationService(true, """
                ```json
                {"residualQuery":"推荐系统","careerStage":"EARLY_CAREER","technicalSkills":["Java"],"currentCity":"上海"}
                ```
                """),
            new ObjectMapper()
        );

        SearchIntentParseResult result = parser.parse("上海 校招 Java 推荐系统");

        assertEquals("推荐系统", result.getResidualQuery());
        assertEquals(CareerStage.EARLY_CAREER, result.getFilter().getCareerStage());
        assertIterableEquals(List.of("Java"), result.getFilter().getTechnicalSkills());
        assertEquals("上海", result.getFilter().getCurrentCity());
    }

    private static class StubLlmGenerationService implements LlmGenerationService {
        private final boolean available;
        private final String response;

        private StubLlmGenerationService(boolean available, String response) {
            this.available = available;
            this.response = response;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public String generate(String systemPrompt, String userPrompt) {
            return response;
        }
    }
}
