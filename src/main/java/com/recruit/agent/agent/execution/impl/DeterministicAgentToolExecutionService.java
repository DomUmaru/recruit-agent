package com.recruit.agent.agent.execution.impl;

import com.recruit.agent.agent.execution.AgentToolExecutionResult;
import com.recruit.agent.agent.execution.AgentToolExecutionService;
import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.agent.tool.CompareCandidatesToolService;
import com.recruit.agent.agent.tool.RefineSearchFilterToolService;
import com.recruit.agent.agent.tool.SearchCandidateToolService;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.chat.state.ChatSessionState;
import com.recruit.agent.comparison.dto.CandidateComparisonRequest;
import com.recruit.agent.search.dto.CandidateSearchRefineRequest;
import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.dto.FilterMergeMode;
import org.springframework.stereotype.Service;

/**
 * 默认确定性工具执行服务。
 */
@Service
public class DeterministicAgentToolExecutionService implements AgentToolExecutionService {

    private final CompareCandidatesToolService compareCandidatesToolService;
    private final SearchCandidateToolService searchCandidateToolService;
    private final RefineSearchFilterToolService refineSearchFilterToolService;

    public DeterministicAgentToolExecutionService(CompareCandidatesToolService compareCandidatesToolService,
                                                  SearchCandidateToolService searchCandidateToolService,
                                                  RefineSearchFilterToolService refineSearchFilterToolService) {
        this.compareCandidatesToolService = compareCandidatesToolService;
        this.searchCandidateToolService = searchCandidateToolService;
        this.refineSearchFilterToolService = refineSearchFilterToolService;
    }

    @Override
    public AgentToolExecutionResult execute(AgentRouteDecision routeDecision, ChatSessionState state, String userInput) {
        AgentToolExecutionResult result = new AgentToolExecutionResult();

        if (routeDecision.getScene() == ChatScene.COMPARE) {
            CandidateComparisonRequest comparisonRequest = new CandidateComparisonRequest();
            comparisonRequest.setCandidateIds(state.getLastCandidateIds());
            comparisonRequest.setTargetQuery(state.getCurrentQuery());
            result.setComparisonResponse(compareCandidatesToolService.execute(comparisonRequest));
            return result;
        }

        if (routeDecision.getScene() == ChatScene.FILTER_REFINE) {
            CandidateSearchRefineRequest refineRequest = new CandidateSearchRefineRequest();
            refineRequest.setBaseRequest(buildBaseRequest(state));
            refineRequest.setRefinementQuery(userInput);
            refineRequest.setMergeMode(FilterMergeMode.APPEND);
            refineRequest.setScopeCandidateIds(state.getLastCandidateIds());
            result.setSearchResponse(refineSearchFilterToolService.execute(refineRequest));
            return result;
        }

        CandidateSearchRequest searchRequest = new CandidateSearchRequest();
        searchRequest.setQuery(userInput);
        searchRequest.setFilter(state.getFilter());
        result.setSearchResponse(searchCandidateToolService.execute(searchRequest));
        return result;
    }

    private CandidateSearchRequest buildBaseRequest(ChatSessionState state) {
        CandidateSearchRequest request = new CandidateSearchRequest();
        request.setQuery(state.getCurrentQuery());
        request.setFilter(state.getFilter());
        request.setScopeCandidateIds(state.getLastCandidateIds());
        return request;
    }
}
