package com.recruit.agent.agent.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruit.agent.agent.execution.AgentToolExecutionResult;
import com.recruit.agent.agent.execution.AgentToolExecutionService;
import com.recruit.agent.agent.selection.CandidateSelectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.agent.orchestrator.dto.AgentExecuteRequest;
import com.recruit.agent.agent.orchestrator.dto.AgentExecuteResponse;
import com.recruit.agent.agent.orchestrator.impl.AgentOrchestratorServiceImpl;
import com.recruit.agent.agent.router.AgentRouterService;
import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.chat.model.ChatMessage;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.chat.model.ChatSession;
import com.recruit.agent.chat.model.ChatSessionStatus;
import com.recruit.agent.chat.repository.ChatMessageRepository;
import com.recruit.agent.chat.repository.ChatSessionRepository;
import com.recruit.agent.chat.state.ChatSessionState;
import com.recruit.agent.chat.state.ChatSessionStateService;
import com.recruit.agent.search.vo.CandidateSearchItemVO;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgentOrchestratorServiceImplTest {

    @Test
    void shouldExecuteSearchFlowAndPersistSessionState() {
        AgentRouterService routerService = org.mockito.Mockito.mock(AgentRouterService.class);
        AgentToolExecutionService agentToolExecutionService = org.mockito.Mockito.mock(AgentToolExecutionService.class);
        CandidateSelectionService candidateSelectionService = org.mockito.Mockito.mock(CandidateSelectionService.class);
        ChatSessionRepository sessionRepository = org.mockito.Mockito.mock(ChatSessionRepository.class);
        ChatMessageRepository messageRepository = org.mockito.Mockito.mock(ChatMessageRepository.class);
        ChatSessionStateService stateService = org.mockito.Mockito.mock(ChatSessionStateService.class);

        AgentOrchestratorServiceImpl service = new AgentOrchestratorServiceImpl(
            routerService,
            agentToolExecutionService,
            candidateSelectionService,
            sessionRepository,
            messageRepository,
            stateService,
            new ObjectMapper()
        );

        ChatSession session = new ChatSession();
        session.setId("session-id");
        session.setSessionNo("session-1");
        session.setUserId("user-1");
        session.setCurrentScene(ChatScene.SEARCH);
        session.setStatus(ChatSessionStatus.ACTIVE);

        ChatSessionState state = new ChatSessionState();
        state.setCurrentScene(ChatScene.SEARCH);

        AgentRouteDecision decision = new AgentRouteDecision();
        decision.setScene(ChatScene.SEARCH);
        decision.setToolName("searchCandidateByJDTool");

        CandidateSearchItemVO item = new CandidateSearchItemVO();
        item.setCandidateId("candidate-1");
        CandidateSearchResponse searchResponse = new CandidateSearchResponse();
        searchResponse.setTotal(1);
        searchResponse.setCandidates(List.of(item));
        AgentToolExecutionResult executionResult = new AgentToolExecutionResult();
        executionResult.setSearchResponse(searchResponse);

        ChatMessage existingMessage = new ChatMessage();
        existingMessage.setSequenceNo(1);

        when(sessionRepository.findBySessionNo("session-1")).thenReturn(Optional.of(session));
        when(stateService.load(session)).thenReturn(state);
        when(routerService.route(any())).thenReturn(decision);
        when(candidateSelectionService.resolveSelectedCandidateIds(any(), any())).thenReturn(null);
        when(agentToolExecutionService.execute(any(), any(), any())).thenReturn(executionResult);
        when(messageRepository.findBySessionIdOrderBySequenceNoAsc("session-id")).thenReturn(List.of(), List.of(existingMessage));
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AgentExecuteRequest request = new AgentExecuteRequest();
        request.setSessionNo("session-1");
        request.setUserId("user-1");
        request.setUserInput("找做推荐系统的候选人");

        AgentExecuteResponse response = service.execute(request);

        assertEquals("session-1", response.getSessionNo());
        assertEquals(1, response.getSearchResponse().getTotal());
        assertNotNull(response.getSummary());
        verify(agentToolExecutionService).execute(any(), any(), any());
        verify(stateService).apply(any(ChatSession.class), any(ChatSessionState.class));
        verify(messageRepository, org.mockito.Mockito.times(2)).save(any(ChatMessage.class));
    }

    @Test
    void shouldApplySelectedCandidateIdsBeforeCompareExecution() {
        AgentRouterService routerService = org.mockito.Mockito.mock(AgentRouterService.class);
        AgentToolExecutionService agentToolExecutionService = org.mockito.Mockito.mock(AgentToolExecutionService.class);
        CandidateSelectionService candidateSelectionService = org.mockito.Mockito.mock(CandidateSelectionService.class);
        ChatSessionRepository sessionRepository = org.mockito.Mockito.mock(ChatSessionRepository.class);
        ChatMessageRepository messageRepository = org.mockito.Mockito.mock(ChatMessageRepository.class);
        ChatSessionStateService stateService = org.mockito.Mockito.mock(ChatSessionStateService.class);

        AgentOrchestratorServiceImpl service = new AgentOrchestratorServiceImpl(
            routerService,
            agentToolExecutionService,
            candidateSelectionService,
            sessionRepository,
            messageRepository,
            stateService,
            new ObjectMapper()
        );

        ChatSession session = new ChatSession();
        session.setId("session-id");
        session.setSessionNo("session-2");
        session.setUserId("user-1");
        session.setCurrentScene(ChatScene.SEARCH);
        session.setStatus(ChatSessionStatus.ACTIVE);

        ChatSessionState state = new ChatSessionState();
        state.setCurrentScene(ChatScene.SEARCH);
        state.setLastCandidateIds(List.of("candidate-1", "candidate-2", "candidate-3"));

        AgentRouteDecision decision = new AgentRouteDecision();
        decision.setScene(ChatScene.COMPARE);
        decision.setToolName("compareCandidatesTool");

        ChatMessage existingMessage = new ChatMessage();
        existingMessage.setSequenceNo(1);

        when(sessionRepository.findBySessionNo("session-2")).thenReturn(Optional.of(session));
        when(stateService.load(session)).thenReturn(state);
        when(routerService.route(any())).thenReturn(decision);
        when(candidateSelectionService.resolveSelectedCandidateIds(any(), eq("对比前两个")))
            .thenReturn(List.of("candidate-1", "candidate-2"));
        when(agentToolExecutionService.execute(any(), any(), any())).thenReturn(new AgentToolExecutionResult());
        when(messageRepository.findBySessionIdOrderBySequenceNoAsc("session-id")).thenReturn(List.of(), List.of(existingMessage));
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AgentExecuteRequest request = new AgentExecuteRequest();
        request.setSessionNo("session-2");
        request.setUserId("user-1");
        request.setUserInput("对比前两个");

        service.execute(request);

        assertEquals(List.of("candidate-1", "candidate-2"), state.getSelectedCandidateIds());
        verify(agentToolExecutionService).execute(any(), eq(state), eq("对比前两个"));
    }
}
