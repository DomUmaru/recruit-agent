package com.recruit.agent.search.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 候选人搜索结果视图。
 */
@Getter
@Setter
@NoArgsConstructor
public class CandidateSearchItemVO {

    private String candidateId;

    private String candidateNo;

    private String fullName;

    private String currentCity;

    private String schoolName;

    private String highestDegree;

    private String schoolTier;

    private BigDecimal totalYearsOfExperience;

    private List<String> technicalSkills;

    private Boolean bigTech;

    private Boolean outsourcing;

    private String profileSummary;

    private double matchScore;

    private Double rerankScore;

    private List<String> matchReasons;

    private List<CandidateSearchEvidenceVO> evidenceList;
}
