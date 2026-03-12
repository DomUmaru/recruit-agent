package com.recruit.agent.agent.tool.impl;

import com.recruit.agent.agent.tool.CompareCandidatesToolService;
import com.recruit.agent.comparison.dto.CandidateComparisonRequest;
import com.recruit.agent.comparison.service.CandidateComparisonService;
import com.recruit.agent.comparison.vo.CandidateComparisonResponse;
import org.springframework.stereotype.Service;

/**
 * 候选人对比工具服务实现。
 */
@Service
public class CompareCandidatesToolServiceImpl implements CompareCandidatesToolService {

    private final CandidateComparisonService candidateComparisonService;

    public CompareCandidatesToolServiceImpl(CandidateComparisonService candidateComparisonService) {
        this.candidateComparisonService = candidateComparisonService;
    }

    @Override
    public CandidateComparisonResponse execute(CandidateComparisonRequest request) {
        return candidateComparisonService.compare(request);
    }
}
