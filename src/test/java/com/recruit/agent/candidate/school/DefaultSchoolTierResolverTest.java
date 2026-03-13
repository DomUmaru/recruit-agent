package com.recruit.agent.candidate.school;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import org.junit.jupiter.api.Test;

class DefaultSchoolTierResolverTest {

    private final DefaultSchoolTierResolver resolver = new DefaultSchoolTierResolver(new ObjectMapper());

    @Test
    void shouldResolveTopTierSchools() {
        assertEquals(SchoolTier.C9, resolver.resolve("上海交通大学", DegreeLevel.BACHELOR));
        assertEquals(SchoolTier.PROJECT_211, resolver.resolve("北京邮电大学", DegreeLevel.BACHELOR));
        assertEquals(SchoolTier.DOUBLE_FIRST_CLASS, resolver.resolve("南方科技大学", DegreeLevel.BACHELOR));
    }

    @Test
    void shouldFallbackToGeneralUndergradAndJuniorCollege() {
        assertEquals(SchoolTier.GENERAL_UNDERGRAD, resolver.resolve("重庆科技大学", DegreeLevel.BACHELOR));
        assertEquals(SchoolTier.JUNIOR_COLLEGE, resolver.resolve("重庆电子科技职业大学", DegreeLevel.ASSOCIATE));
        assertEquals(SchoolTier.JUNIOR_COLLEGE, resolver.resolve("重庆工业职业技术学院", null));
    }

    @Test
    void shouldResolveNullWhenNoSchoolAndNoDegree() {
        assertNull(resolver.resolve(null, null));
    }
}
