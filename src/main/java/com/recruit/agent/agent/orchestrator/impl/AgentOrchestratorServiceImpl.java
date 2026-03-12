package com.recruit.agent.agent.orchestrator.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.agent.execution.AgentToolExecutionResult;
import com.recruit.agent.agent.execution.AgentToolExecutionService;
import com.recruit.agent.agent.orchestrator.AgentOrchestratorService;
import com.recruit.agent.agent.orchestrator.dto.AgentExecuteRequest;
import com.recruit.agent.agent.orchestrator.dto.AgentExecuteResponse;
import com.recruit.agent.agent.router.AgentRouterService;
import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.agent.router.dto.AgentRoutingContext;
import com.recruit.agent.agent.selection.CandidateSelectionService;
import com.recruit.agent.chat.model.ChatMessage;
import com.recruit.agent.chat.model.ChatMessageRole;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.chat.model.ChatSession;
import com.recruit.agent.chat.model.ChatSessionStatus;
import com.recruit.agent.chat.repository.ChatMessageRepository;
import com.recruit.agent.chat.repository.ChatSessionRepository;
import com.recruit.agent.chat.state.ChatSessionState;
import com.recruit.agent.chat.state.ChatSessionStateService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 总编排器。
 * 这是 chat 主链路里最核心的一层，负责把一句用户输入编排成一轮完整执行：
 * 1. 恢复会话和状态
 * 2. 判定当前场景
 * 3. 必要时从历史候选集里做选择
 * 4. 调用 tool execution 执行业务
 * 5. 更新状态并保存消息
 */
@Service
public class AgentOrchestratorServiceImpl implements AgentOrchestratorService {

    private final AgentRouterService agentRouterService;
    private final AgentToolExecutionService agentToolExecutionService;
    private final CandidateSelectionService candidateSelectionService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionStateService chatSessionStateService;
    private final ObjectMapper objectMapper;

    public AgentOrchestratorServiceImpl(AgentRouterService agentRouterService,
                                        AgentToolExecutionService agentToolExecutionService,
                                        CandidateSelectionService candidateSelectionService,
                                        ChatSessionRepository chatSessionRepository,
                                        ChatMessageRepository chatMessageRepository,
                                        ChatSessionStateService chatSessionStateService,
                                        ObjectMapper objectMapper) {
        this.agentRouterService = agentRouterService;
        this.agentToolExecutionService = agentToolExecutionService;
        this.candidateSelectionService = candidateSelectionService;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatSessionStateService = chatSessionStateService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentExecuteResponse execute(AgentExecuteRequest request) {
        AgentExecuteRequest safeRequest = request == null ? new AgentExecuteRequest() : request;

        // 先恢复会话壳和运行时状态；后续 router / selection / execution 都依赖这份状态。
        ChatSession session = findOrCreateSession(safeRequest);
        ChatSessionState state = chatSessionStateService.load(session);

        // 用户输入先落库，保证后续即使执行失败，也能追踪到原始请求。
        saveMessage(session, ChatMessageRole.USER, safeRequest.getUserInput(), null);

        // route 只负责“这句话应该走哪条业务链”，不直接执行业务。
        AgentRouteDecision routeDecision = agentRouterService.route(buildRoutingContext(safeRequest, session));

        // compare / interview 这类场景会先从上轮候选集里解析“前几个、第几个、全部”等选择语义。
        applySelection(state, routeDecision, safeRequest.getUserInput());

        // 统一交给 execution 层执行，屏蔽 deterministic / Spring AI 两套实现差异。
        AgentToolExecutionResult executionResult = agentToolExecutionService.execute(routeDecision, state, safeRequest.getUserInput());

        // 把本轮执行结果写回 state，保证多轮对话可以延续上下文。
        updateState(state, routeDecision, safeRequest.getUserInput(), executionResult);
        chatSessionStateService.apply(session, state);
        chatSessionRepository.save(session);

        String summary = buildSummary(executionResult, routeDecision);

        // assistant 消息里保存本轮总结，同时把 routeDecision 一并落库，方便审计。
        saveMessage(session, ChatMessageRole.ASSISTANT, summary, routeDecision);

        AgentExecuteResponse response = new AgentExecuteResponse();
        response.setSessionNo(session.getSessionNo());
        response.setRouteDecision(routeDecision);
        response.setSearchResponse(executionResult.getSearchResponse());
        response.setComparisonResponse(executionResult.getComparisonResponse());
        response.setInterviewResponse(executionResult.getInterviewResponse());
        response.setSummary(summary);
        return response;
    }

    private ChatSession findOrCreateSession(AgentExecuteRequest request) {
        // 有 sessionNo 时优先复用历史会话；否则新建一个会话。
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
        // router 只需要少量关键信息：本轮输入 + 上轮 query/filter/candidate scope。
        AgentRoutingContext context = new AgentRoutingContext();
        context.setUserInput(request.getUserInput());
        context.setCurrentScene(session.getCurrentScene());
        context.setCurrentQuery(session.getCurrentQuery());
        context.setFiltersJson(session.getFiltersJson());
        context.setLastCandidateIdsJson(session.getLastCandidateIdsJson());
        return context;
    }

    private void applySelection(ChatSessionState state, AgentRouteDecision routeDecision, String userInput) {
        // 只有 compare / interview 需要“从上一轮结果里挑人”，搜索类场景不走这里。
        if (routeDecision.getScene() != ChatScene.COMPARE && routeDecision.getScene() != ChatScene.INTERVIEW) {
            return;
        }

        List<String> selectedCandidateIds = candidateSelectionService.resolveSelectedCandidateIds(state, userInput);
        if (selectedCandidateIds != null && !selectedCandidateIds.isEmpty()) {
            state.setSelectedCandidateIds(selectedCandidateIds);
        }
    }

    private void updateState(ChatSessionState state,
                             AgentRouteDecision routeDecision,
                             String userInput,
                             AgentToolExecutionResult executionResult) {
        // currentScene 代表当前会话正停留在哪条业务链上。
        state.setCurrentScene(routeDecision.getScene());
        state.setCurrentQuery(resolveCurrentQuery(state.getCurrentQuery(), routeDecision.getScene(), userInput));

        // compare / interview 的结果本质上是在当前已选候选人集合上继续操作。
        if (executionResult.getInterviewResponse() != null && executionResult.getInterviewResponse().getCandidates() != null) {
            state.setSelectedCandidateIds(executionResult.getInterviewResponse().getCandidates().stream()
                .map(candidate -> candidate.getCandidateId())
                .toList());
        }
        if (executionResult.getComparisonResponse() != null && executionResult.getComparisonResponse().getCandidates() != null) {
            state.setSelectedCandidateIds(executionResult.getComparisonResponse().getCandidates().stream()
                .map(candidate -> candidate.getCandidateId())
                .toList());
        }

        // 搜索类场景会刷新 lastCandidateIds，并清空之前的 selected 集合，避免旧选择污染新搜索。
        if (executionResult.getSearchResponse() != null && executionResult.getSearchResponse().getCandidates() != null) {
            state.setLastCandidateIds(executionResult.getSearchResponse().getCandidates().stream()
                .map(candidate -> candidate.getCandidateId())
                .toList());
            state.setSelectedCandidateIds(null);
        }
    }

    private String buildSummary(AgentToolExecutionResult executionResult, AgentRouteDecision routeDecision) {
        // compare / interview 直接复用下游服务生成的 summary；搜索场景在这里组装统一话术。
        if (routeDecision.getScene() == ChatScene.INTERVIEW && executionResult.getInterviewResponse() != null) {
            return executionResult.getInterviewResponse().getSummary();
        }

        if (routeDecision.getScene() == ChatScene.COMPARE && executionResult.getComparisonResponse() != null) {
            return executionResult.getComparisonResponse().getSummary();
        }

        int total = executionResult.getSearchResponse() == null ? 0 : executionResult.getSearchResponse().getTotal();
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
        // 当前实现按 session 内历史消息顺序自增，保证消息可以稳定重放。
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

    private String resolveCurrentQuery(String currentQuery, ChatScene scene, String userInput) {
        // refinement 会把新条件拼回历史 query；compare / interview 只消费已有 query，不主动改写。
        if (scene == ChatScene.FILTER_REFINE) {
            return mergeQuery(currentQuery, userInput);
        }
        if (scene == ChatScene.COMPARE || scene == ChatScene.INTERVIEW) {
            return currentQuery == null || currentQuery.isBlank() ? userInput : currentQuery;
        }
        return userInput;
    }
}
