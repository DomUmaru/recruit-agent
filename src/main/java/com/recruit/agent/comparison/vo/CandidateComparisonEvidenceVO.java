package com.recruit.agent.comparison.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 候选人对比证据。
 */
@Getter
@Setter
@NoArgsConstructor
public class CandidateComparisonEvidenceVO {

    private String section;

    private Integer page;

    private String content;
}
