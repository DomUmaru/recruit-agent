package com.recruit.agent.candidate.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import com.recruit.agent.resume.model.ResumeDocument;
import com.recruit.agent.resume.model.ResumeParseType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RuleBasedCandidateProfileExtractorTest {

    @Test
    void shouldExtractEnglishResumeProfile() {
        RuleBasedCandidateProfileExtractor extractor = new RuleBasedCandidateProfileExtractor();

        ResumeDocument document = new ResumeDocument();
        document.setId("doc-1");
        document.setParseType(ResumeParseType.PDF_TEXT);
        document.setCleanedText("""
            Resume Demo
            Java Spring Boot Elasticsearch MySQL
            Bachelor degree 985 school 5 years experience
            Alibaba recommendation system project
            """);

        CandidateProfileDraft draft = extractor.extract(document);

        assertEquals(DegreeLevel.BACHELOR, draft.getHighestDegree());
        assertEquals(SchoolTier.PROJECT_985, draft.getSchoolTier());
        assertEquals(new BigDecimal("5.0"), draft.getTotalYearsOfExperience());
        assertTrue(draft.getTechnicalSkills().contains("Java"));
        assertTrue(draft.getTechnicalSkills().contains("Spring Boot"));
        assertEquals(Boolean.FALSE, draft.getBigTech());
    }
}
