package com.recruit.agent.comparison.vo;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 候选人对比响应。
 */
@Getter
@Setter
@NoArgsConstructor
public class CandidateComparisonResponse {

    private List<CandidateComparisonItemVO> candidates;

    private String summary;
}
