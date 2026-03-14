package com.recruit.agent.chat.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * compare 场景单候选人载荷。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatComparisonCandidatePayload {

    private Integer rank;

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

    private List<ChatComparisonEvidencePayload> evidenceList;
}
