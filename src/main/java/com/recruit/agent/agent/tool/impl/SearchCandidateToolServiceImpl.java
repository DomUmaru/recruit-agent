package com.recruit.agent.agent.tool.impl;

import com.recruit.agent.agent.tool.SearchCandidateToolService;
import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.service.CandidateSearchService;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import org.springframework.stereotype.Service;

/**
 * 候选人搜索工具服务实现。
 */
@Service
public class SearchCandidateToolServiceImpl implements SearchCandidateToolService {

    private final CandidateSearchService candidateSearchService;

    public SearchCandidateToolServiceImpl(CandidateSearchService candidateSearchService) {
        this.candidateSearchService = candidateSearchService;
    }

    @Override
    public CandidateSearchResponse execute(CandidateSearchRequest request) {
        return candidateSearchService.search(request);
    }
}
