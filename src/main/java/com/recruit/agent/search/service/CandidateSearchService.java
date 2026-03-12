package com.recruit.agent.search.service;

import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.vo.CandidateSearchResponse;

/**
 * 候选人搜索服务。
 */
public interface CandidateSearchService {

    /**
     * 执行候选人搜索，返回候选人级结果与证据级片段。
     *
     * @param request 搜索请求
     * @return 搜索结果
     */
    CandidateSearchResponse search(CandidateSearchRequest request);
}
