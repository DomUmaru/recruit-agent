package com.recruit.agent.search.service;

import com.recruit.agent.search.dto.CandidateSearchRefineRequest;
import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.vo.CandidateSearchResponse;

/**
 * 候选人搜索 refinement 服务。
 */
public interface CandidateSearchRefinementService {

    /**
     * 合并上一轮搜索请求与本轮 refinement 条件。
     *
     * @param request refinement 请求
     * @return 合并后的搜索请求
     */
    CandidateSearchRequest merge(CandidateSearchRefineRequest request);

    /**
     * 执行 refinement 搜索。
     *
     * @param request refinement 请求
     * @return 搜索结果
     */
    CandidateSearchResponse refineSearch(CandidateSearchRefineRequest request);
}
