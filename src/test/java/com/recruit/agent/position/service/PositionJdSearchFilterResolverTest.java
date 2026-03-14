package com.recruit.agent.position.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.recruit.agent.candidate.model.CareerStage;
import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.position.model.PositionJD;
import org.junit.jupiter.api.Test;

class PositionJdSearchFilterResolverTest {

    @Test
    void shouldResolveCampusDefaultsFromJd() {
        PositionJD positionJD = new PositionJD();
        positionJD.setTitle("校招 Java 后端开发");
        positionJD.setRawJdText("面向应届毕业生，要求硕士及以上学历");
        positionJD.setRequiredDegree("硕士");
        positionJD.setLocation("上海");
        positionJD.setMinYearsOfExperience(0);

        PositionJdSearchFilterResolver resolver = new PositionJdSearchFilterResolver();
        var filter = resolver.resolve(positionJD);

        assertEquals(CareerStage.EARLY_CAREER, filter.getCareerStage());
        assertEquals(DegreeLevel.MASTER, filter.getHighestDegrees().get(0));
        assertEquals("上海", filter.getCurrentCity());
        assertEquals(0, filter.getMinYearsOfExperience().intValue());
    }
}
