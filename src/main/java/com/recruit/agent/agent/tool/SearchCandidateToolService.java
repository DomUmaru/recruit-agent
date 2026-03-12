package com.recruit.agent.agent.tool;

import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.vo.CandidateSearchResponse;

/**
 * 候选人搜索工具服务。
 */
public interface SearchCandidateToolService {

    /**
     * 执行候选人搜索工具。
     *
     * @param request 搜索请求
     * @return 搜索结果
     */
    CandidateSearchResponse execute(CandidateSearchRequest request);
}
