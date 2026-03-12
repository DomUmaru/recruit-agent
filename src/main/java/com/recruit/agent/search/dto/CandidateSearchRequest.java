package com.recruit.agent.search.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 候选人搜索请求。
 */
@Getter
@Setter
@NoArgsConstructor
public class CandidateSearchRequest {

    /**
     * 自然语言查询或关键词查询。
     */
    private String query;

    /**
     * 结构化过滤条件。
     */
    @Valid
    private CandidateSearchFilter filter = new CandidateSearchFilter();

    /**
     * 候选人范围限定，用于 refinement 或局部重排。
     */
    private List<String> scopeCandidateIds;

    /**
     * 返回候选人数。
     */
    @Min(1)
    @Max(50)
    private Integer limit = 10;

    /**
     * 每个候选人返回的证据条数。
     */
    @Min(0)
    @Max(5)
    private Integer evidenceLimit = 3;
}
