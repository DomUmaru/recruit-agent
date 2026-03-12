package com.recruit.agent.search.normalization;

import com.recruit.agent.search.dto.CandidateSearchRequest;

/**
 * 搜索请求归一化服务。
 */
public interface SearchRequestNormalizationService {

    /**
     * 归一化搜索请求，生成最终用于检索的请求数据。
     *
     * @param request 原始请求
     * @return 归一化后的请求
     */
    PreparedCandidateSearchRequest prepare(CandidateSearchRequest request);
}
