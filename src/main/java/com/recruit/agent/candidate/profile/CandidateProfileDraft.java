package com.recruit.agent.candidate.profile;

import com.recruit.agent.candidate.model.CareerStage;
import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CandidateProfileDraft {

    private CareerStage careerStage;

    private DegreeLevel highestDegree;

    private String schoolName;

    private SchoolTier schoolTier;

    private BigDecimal totalYearsOfExperience;

    private List<String> technicalSkills;

    private List<String> industryTags;

    private List<String> companyTags;

    private List<String> projectTags;

    private Boolean bigTech;

    private Boolean outsourcing;

    private String profileSummary;

    private Map<String, Object> metadata;
}
