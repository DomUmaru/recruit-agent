package com.recruit.agent.comparison.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 单个候选人对比项。
 */
@Getter
@Setter
@NoArgsConstructor
public class CandidateComparisonItemVO {

    private String candidateId;

    private String candidateNo;

    private String fullName;

    private String highestDegree;

    private String schoolTier;

    private BigDecimal totalYearsOfExperience;

    private List<String> technicalSkills;

    private Boolean bigTech;

    private Boolean outsourcing;

    private List<String> highlights;

    private List<String> riskPoints;

    private List<CandidateComparisonEvidenceVO> evidenceList;
}
