package com.recruit.agent.agent.execution.impl;

import com.recruit.agent.agent.execution.AgentToolExecutionResult;
import com.recruit.agent.agent.execution.AgentToolExecutionService;
import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.agent.tool.CompareCandidatesToolService;
import com.recruit.agent.agent.tool.GenerateInterviewQuestionsToolService;
import com.recruit.agent.agent.tool.RefineSearchFilterToolService;
import com.recruit.agent.agent.tool.SearchCandidateToolService;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.chat.state.ChatSessionState;
import com.recruit.agent.comparison.dto.CandidateComparisonRequest;
import com.recruit.agent.interview.dto.InterviewQuestionRequest;
import com.recruit.agent.search.dto.CandidateSearchRefineRequest;
import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.dto.FilterMergeMode;
import org.springframework.stereotype.Service;

/**
 * 默认的确定性 tool 执行器。
 * 特点是所有 request 都由代码构造，适合稳定、可审计、可测试的主链路。
 */
@Service
public class DeterministicAgentToolExecutionService implements AgentToolExecutionService {

    private final CompareCandidatesToolService compareCandidatesToolService;
    private final GenerateInterviewQuestionsToolService generateInterviewQuestionsToolService;
    private final SearchCandidateToolService searchCandidateToolService;
    private final RefineSearchFilterToolService refineSearchFilterToolService;

    public DeterministicAgentToolExecutionService(CompareCandidatesToolService compareCandidatesToolService,
                                                  GenerateInterviewQuestionsToolService generateInterviewQuestionsToolService,
                                                  SearchCandidateToolService searchCandidateToolService,
                                                  RefineSearchFilterToolService refineSearchFilterToolService) {
        this.compareCandidatesToolService = compareCandidatesToolService;
        this.generateInterviewQuestionsToolService = generateInterviewQuestionsToolService;
        this.searchCandidateToolService = searchCandidateToolService;
        this.refineSearchFilterToolService = refineSearchFilterToolService;
    }

    @Override
    public AgentToolExecutionResult execute(AgentRouteDecision routeDecision, ChatSessionState state, String userInput) {
        AgentToolExecutionResult result = new AgentToolExecutionResult();

        // compare/interview 都优先消费当前 selectedCandidateIds；没有时再退回 lastCandidateIds。
        if (routeDecision.getScene() == ChatScene.COMPARE) {
            CandidateComparisonRequest comparisonRequest = new CandidateComparisonRequest();
            comparisonRequest.setCandidateIds(resolveSelectedOrLastCandidateIds(state));
            comparisonRequest.setTargetQuery(state.getCurrentQuery());
            result.setComparisonResponse(compareCandidatesToolService.execute(comparisonRequest));
            return result;
        }

        if (routeDecision.getScene() == ChatScene.INTERVIEW) {
            InterviewQuestionRequest interviewRequest = new InterviewQuestionRequest();
            interviewRequest.setCandidateIds(resolveInterviewCandidateIds(state));
            interviewRequest.setTargetQuery(state.getCurrentQuery());
            result.setInterviewResponse(generateInterviewQuestionsToolService.execute(interviewRequest));
            return result;
        }

        // refinement 是“基于上一轮 request 再追加条件”，不是一轮独立新搜索。
        if (routeDecision.getScene() == ChatScene.FILTER_REFINE) {
            CandidateSearchRefineRequest refineRequest = new CandidateSearchRefineRequest();
            refineRequest.setBaseRequest(buildBaseRequest(state));
            refineRequest.setRefinementQuery(userInput);
            refineRequest.setMergeMode(FilterMergeMode.APPEND);
            refineRequest.setScopeCandidateIds(state.getLastCandidateIds());
            result.setSearchResponse(refineSearchFilterToolService.execute(refineRequest));
            return result;
        }

        // 默认 search 会把当前输入当成 query，并沿用 state 中已有 filter。
        CandidateSearchRequest searchRequest = new CandidateSearchRequest();
        searchRequest.setPositionId(state.getPositionId());
        searchRequest.setQuery(userInput);
        searchRequest.setFilter(state.getFilter());
        result.setSearchResponse(searchCandidateToolService.execute(searchRequest));
        return result;
    }

    private java.util.List<String> resolveInterviewCandidateIds(ChatSessionState state) {
        return resolveSelectedOrLastCandidateIds(state);
    }

    private java.util.List<String> resolveSelectedOrLastCandidateIds(ChatSessionState state) {
        if (state.getSelectedCandidateIds() != null && !state.getSelectedCandidateIds().isEmpty()) {
            return state.getSelectedCandidateIds();
        }
        return state.getLastCandidateIds();
    }

    private CandidateSearchRequest buildBaseRequest(ChatSessionState state) {
        // 把当前 state 还原成一份可继续 refinement 的基础搜索请求。
        CandidateSearchRequest request = new CandidateSearchRequest();
        request.setPositionId(state.getPositionId());
        request.setQuery(state.getCurrentQuery());
        request.setFilter(state.getFilter());
        request.setScopeCandidateIds(state.getLastCandidateIds());
        return request;
    }
}
