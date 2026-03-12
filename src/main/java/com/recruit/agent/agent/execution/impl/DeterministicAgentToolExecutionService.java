package com.recruit.agent.agent.execution.impl;

import com.recruit.agent.agent.execution.AgentToolExecutionService;
import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.agent.tool.RefineSearchFilterToolService;
import com.recruit.agent.agent.tool.SearchCandidateToolService;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.chat.state.ChatSessionState;
import com.recruit.agent.search.dto.CandidateSearchRefineRequest;
import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.dto.FilterMergeMode;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import org.springframework.stereotype.Service;

/**
 * 默认确定性工具执行服务。
 */
@Service
public class DeterministicAgentToolExecutionService implements AgentToolExecutionService {

    private final SearchCandidateToolService searchCandidateToolService;
    private final RefineSearchFilterToolService refineSearchFilterToolService;

    public DeterministicAgentToolExecutionService(SearchCandidateToolService searchCandidateToolService,
                                                  RefineSearchFilterToolService refineSearchFilterToolService) {
        this.searchCandidateToolService = searchCandidateToolService;
        this.refineSearchFilterToolService = refineSearchFilterToolService;
    }

    @Override
    public CandidateSearchResponse execute(AgentRouteDecision routeDecision, ChatSessionState state, String userInput) {
        if (routeDecision.getScene() == ChatScene.FILTER_REFINE) {
            CandidateSearchRefineRequest refineRequest = new CandidateSearchRefineRequest();
            refineRequest.setBaseRequest(buildBaseRequest(state));
            refineRequest.setRefinementQuery(userInput);
            refineRequest.setMergeMode(FilterMergeMode.APPEND);
            refineRequest.setScopeCandidateIds(state.getLastCandidateIds());
            return refineSearchFilterToolService.execute(refineRequest);
        }

        CandidateSearchRequest searchRequest = new CandidateSearchRequest();
        searchRequest.setQuery(userInput);
        searchRequest.setFilter(state.getFilter());
        return searchCandidateToolService.execute(searchRequest);
    }

    private CandidateSearchRequest buildBaseRequest(ChatSessionState state) {
        CandidateSearchRequest request = new CandidateSearchRequest();
        request.setQuery(state.getCurrentQuery());
        request.setFilter(state.getFilter());
        request.setScopeCandidateIds(state.getLastCandidateIds());
        return request;
    }
}
