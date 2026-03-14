package com.recruit.agent.agent.tool.impl;

import com.recruit.agent.agent.tool.RefineSearchFilterToolService;
import com.recruit.agent.search.dto.CandidateSearchRefineRequest;
import com.recruit.agent.search.service.CandidateSearchRefinementService;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import org.springframework.stereotype.Service;

/**
 * 搜索筛选 refinement 工具服务实现。
 */
@Service
public class RefineSearchFilterToolServiceImpl implements RefineSearchFilterToolService {

    private final CandidateSearchRefinementService candidateSearchRefinementService;

    public RefineSearchFilterToolServiceImpl(CandidateSearchRefinementService candidateSearchRefinementService) {
        this.candidateSearchRefinementService = candidateSearchRefinementService;
    }

    @Override
    public CandidateSearchResponse execute(CandidateSearchRefineRequest request) {
        return candidateSearchRefinementService.refineSearch(request);
    }
}
