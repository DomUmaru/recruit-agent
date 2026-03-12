package com.recruit.agent.search.vo;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 候选人搜索响应。
 */
@Getter
@Setter
@NoArgsConstructor
public class CandidateSearchResponse {

    private String query;

    private int total;

    private List<CandidateSearchItemVO> candidates;
}
