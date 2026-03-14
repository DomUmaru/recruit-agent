package com.recruit.agent.search.parser.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import com.recruit.agent.llm.LlmGenerationService;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import com.recruit.agent.search.parser.SearchIntentParseResult;
import com.recruit.agent.search.parser.SearchIntentParser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LlmSearchIntentParser implements SearchIntentParser {

    private static final Logger log = LoggerFactory.getLogger(LlmSearchIntentParser.class);

    private final LlmGenerationService llmGenerationService;
    private final ObjectMapper objectMapper;

    public LlmSearchIntentParser(LlmGenerationService llmGenerationService, ObjectMapper objectMapper) {
        this.llmGenerationService = llmGenerationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isAvailable() {
        return llmGenerationService.isAvailable();
    }

    @Override
    public SearchIntentParseResult parse(String text) {
        if (!isAvailable() || text == null || text.isBlank()) {
            return emptyResult();
        }

        try {
            String response = llmGenerationService.generate(buildSystemPrompt(), buildUserPrompt(text));
            if (response == null || response.isBlank()) {
                return emptyResult();
            }

            LlmSearchIntentOutput output = objectMapper.readValue(extractJson(response), LlmSearchIntentOutput.class);
            SearchIntentParseResult result = new SearchIntentParseResult();
            result.setResidualQuery(safeText(output.getResidualQuery()));
            result.setFilter(toFilter(output));
            return result;
        } catch (Exception exception) {
            log.warn("LLM search intent parsing failed. Falling back to rule-based parser.", exception);
            return emptyResult();
        }
    }

    private String buildSystemPrompt() {
        return """
            You parse a recruitment search query into a residual keyword query and a structured filter.
            Return JSON only with this schema:
            {
              "residualQuery":"string",
              "highestDegrees":["BACHELOR|MASTER|DOCTOR|ASSOCIATE|HIGH_SCHOOL"],
              "schoolTiers":["PROJECT_985|PROJECT_211|DOUBLE_FIRST_CLASS|C9|OVERSEAS_TOP"],
              "minYearsOfExperience": number,
              "technicalSkills":["string"],
              "currentCity":"string",
              "bigTech": true|false|null,
              "outsourcing": true|false|null
            }

            Rules:
            - residualQuery must preserve the main retrieval topic keywords and should not over-summarize.
            - Keep technical/domain/topic words in residualQuery whenever they are part of the search intent, such as:
              Java, Go, C++, Python, Spring Boot, Elasticsearch, Redis, MySQL, Kafka,
              搜索, 推荐, 广告, 风控, 后端, 前端, 算法, 推荐系统.
            - Move clearly structured constraints into filter fields when possible:
              degree, school tier, years of experience, current city, bigTech, outsourcing.
            - If the query contains both topic words and structured filters, residualQuery should keep the topic words.
            - Do not invent skills, degrees, cities or years not implied by the input.
            - If unknown, use null or empty arrays.
            - Return valid JSON only.
            """;
    }

    private String buildUserPrompt(String text) {
        return "User query:\n" + text;
    }

    private CandidateSearchFilter toFilter(LlmSearchIntentOutput output) {
        CandidateSearchFilter filter = new CandidateSearchFilter();
        filter.setHighestDegrees(parseDegrees(output.getHighestDegrees()));
        filter.setSchoolTiers(parseSchoolTiers(output.getSchoolTiers()));
        filter.setMinYearsOfExperience(output.getMinYearsOfExperience());
        filter.setTechnicalSkills(emptyToNull(output.getTechnicalSkills()));
        filter.setCurrentCity(blankToNull(output.getCurrentCity()));
        filter.setBigTech(output.getBigTech());
        filter.setOutsourcing(output.getOutsourcing());
        return filter;
    }

    private List<DegreeLevel> parseDegrees(List<String> values) {
        return values == null ? null : values.stream()
            .map(value -> parseEnum(value, DegreeLevel.class))
            .filter(value -> value != null)
            .distinct()
            .toList();
    }

    private List<SchoolTier> parseSchoolTiers(List<String> values) {
        return values == null ? null : values.stream()
            .map(value -> parseEnum(value, SchoolTier.class))
            .filter(value -> value != null)
            .distinct()
            .toList();
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> type) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private SearchIntentParseResult emptyResult() {
        return new SearchIntentParseResult();
    }

    private String extractJson(String response) {
        String trimmed = response == null ? "" : response.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            if (firstLineEnd >= 0) {
                trimmed = trimmed.substring(firstLineEnd + 1).trim();
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }

        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd >= objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1);
        }
        return trimmed;
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    private String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private List<String> emptyToNull(List<String> values) {
        return values == null || values.isEmpty() ? null : values;
    }

    private static class LlmSearchIntentOutput {
        private String residualQuery;
        private List<String> highestDegrees;
        private List<String> schoolTiers;
        private BigDecimal minYearsOfExperience;
        private List<String> technicalSkills;
        private String currentCity;
        private Boolean bigTech;
        private Boolean outsourcing;

        public String getResidualQuery() {
            return residualQuery;
        }

        public void setResidualQuery(String residualQuery) {
            this.residualQuery = residualQuery;
        }

        public List<String> getHighestDegrees() {
            return highestDegrees;
        }

        public void setHighestDegrees(List<String> highestDegrees) {
            this.highestDegrees = highestDegrees;
        }

        public List<String> getSchoolTiers() {
            return schoolTiers;
        }

        public void setSchoolTiers(List<String> schoolTiers) {
            this.schoolTiers = schoolTiers;
        }

        public BigDecimal getMinYearsOfExperience() {
            return minYearsOfExperience;
        }

        public void setMinYearsOfExperience(BigDecimal minYearsOfExperience) {
            this.minYearsOfExperience = minYearsOfExperience;
        }

        public List<String> getTechnicalSkills() {
            return technicalSkills;
        }

        public void setTechnicalSkills(List<String> technicalSkills) {
            this.technicalSkills = technicalSkills;
        }

        public String getCurrentCity() {
            return currentCity;
        }

        public void setCurrentCity(String currentCity) {
            this.currentCity = currentCity;
        }

        public Boolean getBigTech() {
            return bigTech;
        }

        public void setBigTech(Boolean bigTech) {
            this.bigTech = bigTech;
        }

        public Boolean getOutsourcing() {
            return outsourcing;
        }

        public void setOutsourcing(Boolean outsourcing) {
            this.outsourcing = outsourcing;
        }
    }
}
