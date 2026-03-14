package com.recruit.agent.agent.execution;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.agent.execution.impl.DeterministicAgentToolExecutionService;
import com.recruit.agent.agent.execution.impl.SpringAiAgentToolExecutionService;
import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.agent.tool.springai.CompareCandidatesTools;
import com.recruit.agent.agent.tool.springai.InterviewQuestionTools;
import com.recruit.agent.agent.tool.springai.RefineSearchTools;
import com.recruit.agent.agent.tool.springai.SearchCandidateTools;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.chat.state.ChatSessionState;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class SpringAiAgentToolExecutionServiceTest {

    @Test
    void shouldBypassSpringAiForCompareAndInterviewScenes() {
        ChatClient chatClient = org.mockito.Mockito.mock(ChatClient.class);
        DeterministicAgentToolExecutionService deterministic = org.mockito.Mockito.mock(DeterministicAgentToolExecutionService.class);
        CompareCandidatesTools compareTools = org.mockito.Mockito.mock(CompareCandidatesTools.class);
        InterviewQuestionTools interviewTools = org.mockito.Mockito.mock(InterviewQuestionTools.class);
        SearchCandidateTools searchTools = org.mockito.Mockito.mock(SearchCandidateTools.class);
        RefineSearchTools refineTools = org.mockito.Mockito.mock(RefineSearchTools.class);
        SpringAiAgentToolExecutionService service = new SpringAiAgentToolExecutionService(
            chatClient,
            deterministic,
            compareTools,
            interviewTools,
            searchTools,
            refineTools,
            new ObjectMapper()
        );

        ChatSessionState state = new ChatSessionState();
        AgentRouteDecision compareDecision = new AgentRouteDecision();
        compareDecision.setScene(ChatScene.COMPARE);
        AgentRouteDecision interviewDecision = new AgentRouteDecision();
        interviewDecision.setScene(ChatScene.INTERVIEW);

        AgentToolExecutionResult compareResult = new AgentToolExecutionResult();
        AgentToolExecutionResult interviewResult = new AgentToolExecutionResult();
        when(deterministic.execute(eq(compareDecision), eq(state), eq("compare"))).thenReturn(compareResult);
        when(deterministic.execute(eq(interviewDecision), eq(state), eq("interview"))).thenReturn(interviewResult);

        assertSame(compareResult, service.execute(compareDecision, state, "compare"));
        assertSame(interviewResult, service.execute(interviewDecision, state, "interview"));

        verify(deterministic).execute(compareDecision, state, "compare");
        verify(deterministic).execute(interviewDecision, state, "interview");
        verify(chatClient, never()).prompt();
        verify(searchTools, never()).searchCandidateByJDTool(any());
        verify(refineTools, never()).refineSearchFilterTool(any());
    }
}
