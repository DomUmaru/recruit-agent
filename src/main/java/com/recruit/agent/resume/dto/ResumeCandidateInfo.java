package com.recruit.agent.resume.dto;

import com.recruit.agent.candidate.model.CandidateSource;
import com.recruit.agent.candidate.model.DegreeLevel;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumeCandidateInfo {

    @NotBlank(message = "fullName cannot be blank")
    private String fullName;

    private String phone;

    private String email;

    private String currentCity;

    private String currentCompany;

    private String currentTitle;

    private BigDecimal totalYearsOfExperience;

    private DegreeLevel highestDegree;

    private String schoolName;

    private CandidateSource source;

    private String summary;
}
