package com.recruit.agent.agent.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruit.agent.agent.execution.impl.DeterministicAgentToolExecutionService;
import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.agent.tool.CompareCandidatesToolService;
import com.recruit.agent.agent.tool.GenerateInterviewQuestionsToolService;
import com.recruit.agent.agent.tool.RefineSearchFilterToolService;
import com.recruit.agent.agent.tool.SearchCandidateToolService;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.chat.state.ChatSessionState;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import com.recruit.agent.search.dto.CandidateSearchRefineRequest;
import com.recruit.agent.search.dto.FilterMergeMode;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeterministicAgentToolExecutionServiceTest {

    @Test
    void shouldUseReplaceModeForResetRefinementIntent() {
        CompareCandidatesToolService compareService = org.mockito.Mockito.mock(CompareCandidatesToolService.class);
        GenerateInterviewQuestionsToolService interviewService = org.mockito.Mockito.mock(GenerateInterviewQuestionsToolService.class);
        SearchCandidateToolService searchService = org.mockito.Mockito.mock(SearchCandidateToolService.class);
        RefineSearchFilterToolService refineService = org.mockito.Mockito.mock(RefineSearchFilterToolService.class);
        DeterministicAgentToolExecutionService service = new DeterministicAgentToolExecutionService(
            compareService,
            interviewService,
            searchService,
            refineService
        );

        ChatSessionState state = new ChatSessionState();
        state.setCurrentScene(ChatScene.SEARCH);
        state.setCurrentQuery("Java 后端");
        state.setLastCandidateIds(List.of("candidate-1", "candidate-2"));
        state.setFilter(new CandidateSearchFilter());

        AgentRouteDecision decision = new AgentRouteDecision();
        decision.setScene(ChatScene.FILTER_REFINE);
        decision.setToolName("refineSearchFilterTool");

        CandidateSearchResponse response = new CandidateSearchResponse();
        when(refineService.execute(argThat(request ->
            request.getMergeMode() == FilterMergeMode.REPLACE
                && "Java 后端".equals(request.getBaseRequest().getQuery())
        ))).thenReturn(response);

        AgentToolExecutionResult result = service.execute(decision, state, "重新帮我筛选一次，这次按北京985硕士");

        assertEquals(response, result.getSearchResponse());
        verify(refineService).execute(argThat((CandidateSearchRefineRequest request) ->
            request.getMergeMode() == FilterMergeMode.REPLACE
                && "Java 后端".equals(request.getBaseRequest().getQuery())
                && List.of("candidate-1", "candidate-2").equals(request.getScopeCandidateIds())
        ));
    }
}
