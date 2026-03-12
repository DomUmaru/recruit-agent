package com.recruit.agent.search.normalization;

import com.recruit.agent.search.dto.CandidateSearchFilter;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 归一化后的搜索请求。
 */
@Getter
@Setter
@NoArgsConstructor
public class PreparedCandidateSearchRequest {

    private String rawQuery;

    private String query;

    private CandidateSearchFilter filter;

    private List<String> scopeCandidateIds;

    private int limit;

    private int evidenceLimit;
}
