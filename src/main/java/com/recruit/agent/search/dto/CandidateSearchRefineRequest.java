package com.recruit.agent.search.dto;

import jakarta.validation.Valid;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 候选人搜索 refinement 请求。
 */
@Getter
@Setter
@NoArgsConstructor
public class CandidateSearchRefineRequest {

    /**
     * 上一轮搜索请求。
     */
    @Valid
    private CandidateSearchRequest baseRequest = new CandidateSearchRequest();

    /**
     * 新增或替换的查询语句。
     */
    private String refinementQuery;

    /**
     * 新增或替换的筛选条件。
     */
    @Valid
    private CandidateSearchFilter refinementFilter = new CandidateSearchFilter();

    /**
     * 筛选条件合并策略。
     */
    private FilterMergeMode mergeMode = FilterMergeMode.APPEND;

    /**
     * refinement 范围限定；如果传入则覆盖上一轮 scope。
     */
    private List<String> scopeCandidateIds;
}
