package com.recruit.agent.agent.orchestrator.impl;

import com.recruit.agent.agent.execution.AgentToolExecutionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.agent.orchestrator.AgentOrchestratorService;
import com.recruit.agent.agent.orchestrator.dto.AgentExecuteRequest;
import com.recruit.agent.agent.orchestrator.dto.AgentExecuteResponse;
import com.recruit.agent.agent.router.AgentRouterService;
import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.agent.router.dto.AgentRoutingContext;
import com.recruit.agent.chat.model.ChatMessage;
import com.recruit.agent.chat.model.ChatMessageRole;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.chat.model.ChatSession;
import com.recruit.agent.chat.model.ChatSessionStatus;
import com.recruit.agent.chat.repository.ChatMessageRepository;
import com.recruit.agent.chat.repository.ChatSessionRepository;
import com.recruit.agent.chat.state.ChatSessionState;
import com.recruit.agent.chat.state.ChatSessionStateService;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 编排服务实现。
 */
@Service
public class AgentOrchestratorServiceImpl implements AgentOrchestratorService {

    private final AgentRouterService agentRouterService;
    private final AgentToolExecutionService agentToolExecutionService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionStateService chatSessionStateService;
    private final ObjectMapper objectMapper;

    public AgentOrchestratorServiceImpl(AgentRouterService agentRouterService,
                                        AgentToolExecutionService agentToolExecutionService,
                                        ChatSessionRepository chatSessionRepository,
                                        ChatMessageRepository chatMessageRepository,
                                        ChatSessionStateService chatSessionStateService,
                                        ObjectMapper objectMapper) {
        this.agentRouterService = agentRouterService;
        this.agentToolExecutionService = agentToolExecutionService;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatSessionStateService = chatSessionStateService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentExecuteResponse execute(AgentExecuteRequest request) {
        AgentExecuteRequest safeRequest = request == null ? new AgentExecuteRequest() : request;
        ChatSession session = findOrCreateSession(safeRequest);
        ChatSessionState state = chatSessionStateService.load(session);

        saveMessage(session, ChatMessageRole.USER, safeRequest.getUserInput(), null);

        AgentRouteDecision routeDecision = agentRouterService.route(buildRoutingContext(safeRequest, session));
        CandidateSearchResponse searchResponse = agentToolExecutionService.execute(routeDecision, state, safeRequest.getUserInput());

        updateState(state, routeDecision, safeRequest.getUserInput(), searchResponse);
        chatSessionStateService.apply(session, state);
        chatSessionRepository.save(session);

        String summary = buildSummary(searchResponse, routeDecision);
        saveMessage(session, ChatMessageRole.ASSISTANT, summary, routeDecision);

        AgentExecuteResponse response = new AgentExecuteResponse();
        response.setSessionNo(session.getSessionNo());
        response.setRouteDecision(routeDecision);
        response.setSearchResponse(searchResponse);
        response.setSummary(summary);
        return response;
    }

    private ChatSession findOrCreateSession(AgentExecuteRequest request) {
        if (request.getSessionNo() != null && !request.getSessionNo().isBlank()) {
            return chatSessionRepository.findBySessionNo(request.getSessionNo())
                .orElseGet(() -> createSession(request));
        }
        return createSession(request);
    }

    private ChatSession createSession(AgentExecuteRequest request) {
        ChatSession session = new ChatSession();
        session.setSessionNo(request.getSessionNo() == null || request.getSessionNo().isBlank() ? UUID.randomUUID().toString() : request.getSessionNo());
        session.setUserId(request.getUserId() == null || request.getUserId().isBlank() ? "anonymous" : request.getUserId());
        session.setCurrentScene(ChatScene.SEARCH);
        session.setStatus(ChatSessionStatus.ACTIVE);
        return chatSessionRepository.save(session);
    }

    private AgentRoutingContext buildRoutingContext(AgentExecuteRequest request, ChatSession session) {
        AgentRoutingContext context = new AgentRoutingContext();
        context.setUserInput(request.getUserInput());
        context.setCurrentScene(session.getCurrentScene());
        context.setCurrentQuery(session.getCurrentQuery());
        context.setFiltersJson(session.getFiltersJson());
        context.setLastCandidateIdsJson(session.getLastCandidateIdsJson());
        return context;
    }

    private void updateState(ChatSessionState state,
                             AgentRouteDecision routeDecision,
                             String userInput,
                             CandidateSearchResponse searchResponse) {
        state.setCurrentScene(routeDecision.getScene());
        state.setCurrentQuery(routeDecision.getScene() == ChatScene.FILTER_REFINE
            ? mergeQuery(state.getCurrentQuery(), userInput)
            : userInput);
        state.setLastCandidateIds(searchResponse == null || searchResponse.getCandidates() == null
            ? List.of()
            : searchResponse.getCandidates().stream().map(candidate -> candidate.getCandidateId()).toList());
    }

    private String buildSummary(CandidateSearchResponse response, AgentRouteDecision routeDecision) {
        int total = response == null ? 0 : response.getTotal();
        if (routeDecision.getScene() == ChatScene.FILTER_REFINE) {
            return "已根据最新筛选条件完成 refinement，返回 " + total + " 位候选人。";
        }
        return "已完成候选人搜索，返回 " + total + " 位候选人。";
    }

    private void saveMessage(ChatSession session,
                             ChatMessageRole role,
                             String content,
                             AgentRouteDecision routeDecision) {
        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setRole(role);
        message.setSequenceNo(nextSequenceNo(session.getId()));
        message.setContent(content == null ? "" : content);
        if (routeDecision != null) {
            message.setToolPayloadJson(writeAsJson(routeDecision));
        }
        chatMessageRepository.save(message);
    }

    private Integer nextSequenceNo(String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderBySequenceNoAsc(sessionId);
        if (messages.isEmpty()) {
            return 1;
        }
        return messages.get(messages.size() - 1).getSequenceNo() + 1;
    }

    private String writeAsJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化消息载荷失败", ex);
        }
    }

    private String mergeQuery(String currentQuery, String latestInput) {
        if (currentQuery == null || currentQuery.isBlank()) {
            return latestInput;
        }
        if (latestInput == null || latestInput.isBlank()) {
            return currentQuery;
        }
        return currentQuery + " " + latestInput;
    }
}
