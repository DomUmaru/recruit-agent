package com.recruit.agent.agent.tool;

import com.recruit.agent.search.dto.CandidateSearchRefineRequest;
import com.recruit.agent.search.vo.CandidateSearchResponse;

/**
 * 搜索筛选 refinement 工具服务。
 */
public interface RefineSearchFilterToolService {

    /**
     * 执行 refinement 工具。
     *
     * @param request refinement 请求
     * @return 搜索结果
     */
    CandidateSearchResponse execute(CandidateSearchRefineRequest request);
}
