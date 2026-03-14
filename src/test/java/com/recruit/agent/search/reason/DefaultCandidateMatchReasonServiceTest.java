package com.recruit.agent.search.reason;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.recruit.agent.rag.model.CandidateProfileIndex;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import com.recruit.agent.search.reason.impl.DefaultCandidateMatchReasonService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultCandidateMatchReasonServiceTest {

    @Test
    void shouldBuildReasonsFromQueryAndFilterHits() {
        DefaultCandidateMatchReasonService service = new DefaultCandidateMatchReasonService();

        CandidateProfileIndex profile = new CandidateProfileIndex();
        profile.setTechnicalSkills(List.of("Java", "Elasticsearch"));
        profile.setProjectTags(List.of("推荐系统"));
        profile.setProfileSummary("具备推荐系统与搜索系统经验");
        profile.setTotalYearsOfExperience(new BigDecimal("5.0"));
        profile.setBigTech(true);

        CandidateSearchFilter filter = new CandidateSearchFilter();
        filter.setMinYearsOfExperience(new BigDecimal("3.0"));
        filter.setTechnicalSkills(List.of("Java"));
        filter.setBigTech(true);

        List<String> reasons = service.buildMatchReasons(profile, "java", List.of("java"), filter);

        assertTrue(reasons.contains("技术栈命中查询关键词"));
        assertTrue(reasons.contains("工作年限满足筛选条件"));
        assertTrue(reasons.contains("具备大厂背景"));
        assertTrue(reasons.contains("满足技术栈过滤条件"));
    }

    @Test
    void shouldFallbackToStructuredReasonWhenNoQueryReasonExists() {
        DefaultCandidateMatchReasonService service = new DefaultCandidateMatchReasonService();

        CandidateProfileIndex profile = new CandidateProfileIndex();
        CandidateSearchFilter filter = new CandidateSearchFilter();

        List<String> reasons = service.buildMatchReasons(profile, "", List.of(), filter);

        assertEquals(List.of("满足结构化过滤条件"), reasons);
    }
}
