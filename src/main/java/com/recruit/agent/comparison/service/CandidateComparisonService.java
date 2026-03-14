package com.recruit.agent.comparison.service;

import com.recruit.agent.comparison.dto.CandidateComparisonRequest;
import com.recruit.agent.comparison.vo.CandidateComparisonResponse;

/**
 * 候选人对比服务。
 */
public interface CandidateComparisonService {

    /**
     * 对多个候选人进行结构化对比。
     *
     * @param request 对比请求
     * @return 对比结果
     */
    CandidateComparisonResponse compare(CandidateComparisonRequest request);
}
