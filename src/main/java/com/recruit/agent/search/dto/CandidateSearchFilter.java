package com.recruit.agent.search.dto;

import com.recruit.agent.candidate.model.CareerStage;
import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CandidateSearchFilter {

    private CareerStage careerStage;

    private List<DegreeLevel> highestDegrees;

    private List<SchoolTier> schoolTiers;

    private BigDecimal minYearsOfExperience;

    private List<String> technicalSkills;

    private String currentCity;

    private Boolean bigTech;

    private Boolean outsourcing;
}
