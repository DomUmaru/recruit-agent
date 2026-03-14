package com.recruit.agent.agent.tool;

import com.recruit.agent.comparison.dto.CandidateComparisonRequest;
import com.recruit.agent.comparison.vo.CandidateComparisonResponse;

/**
 * 候选人对比工具服务。
 */
public interface CompareCandidatesToolService {

    /**
     * 执行候选人对比。
     *
     * @param request 对比请求
     * @return 对比结果
     */
    CandidateComparisonResponse execute(CandidateComparisonRequest request);
}
