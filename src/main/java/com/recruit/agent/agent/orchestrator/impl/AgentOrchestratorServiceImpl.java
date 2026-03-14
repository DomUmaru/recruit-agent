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
import com.recruit.agent.position.model.PositionJD;
import com.recruit.agent.position.repository.PositionJDRepository;
import com.recruit.agent.position.service.PositionJdSearchFilterResolver;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Main chat orchestrator.
 */
@Service
public class AgentOrchestratorServiceImpl implements AgentOrchestratorService {

    private final AgentRouterService agentRouterService;
    private final AgentToolExecutionService agentToolExecutionService;
    private final CandidateSelectionService candidateSelectionService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionStateService chatSessionStateService;
    private final PositionJDRepository positionJDRepository;
    private final PositionJdSearchFilterResolver positionJdSearchFilterResolver;
    private final ObjectMapper objectMapper;

    public AgentOrchestratorServiceImpl(AgentRouterService agentRouterService,
                                        AgentToolExecutionService agentToolExecutionService,
                                        CandidateSelectionService candidateSelectionService,
                                        ChatSessionRepository chatSessionRepository,
                                        ChatMessageRepository chatMessageRepository,
                                        ChatSessionStateService chatSessionStateService,
                                        PositionJDRepository positionJDRepository,
                                        PositionJdSearchFilterResolver positionJdSearchFilterResolver,
                                        ObjectMapper objectMapper) {
        this.agentRouterService = agentRouterService;
        this.agentToolExecutionService = agentToolExecutionService;
        this.candidateSelectionService = candidateSelectionService;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatSessionStateService = chatSessionStateService;
        this.positionJDRepository = positionJDRepository;
        this.positionJdSearchFilterResolver = positionJdSearchFilterResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentExecuteResponse execute(AgentExecuteRequest request) {
        AgentExecuteRequest safeRequest = request == null ? new AgentExecuteRequest() : request;

        ChatSession session = findOrCreateSession(safeRequest);
        ChatSessionState state = chatSessionStateService.load(session);
        bindPositionContext(session, state, safeRequest);

        saveMessage(session, ChatMessageRole.USER, safeRequest.getUserInput(), null);

        AgentRouteDecision routeDecision = agentRouterService.route(buildRoutingContext(safeRequest, session));
        applySelection(state, routeDecision, safeRequest.getUserInput());

        AgentToolExecutionResult executionResult = agentToolExecutionService.execute(routeDecision, state, safeRequest.getUserInput());

        updateState(state, routeDecision, safeRequest.getUserInput(), executionResult);
        chatSessionStateService.apply(session, state);
        chatSessionRepository.save(session);

        String summary = buildSummary(executionResult, routeDecision);
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
        if (request.getSessionNo() != null && !request.getSessionNo().isBlank()) {
            return chatSessionRepository.findBySessionNo(request.getSessionNo())
                .orElseGet(() -> createSession(request));
        }
        return createSession(request);
    }

    private ChatSession createSession(AgentExecuteRequest request) {
        ChatSession session = new ChatSession();
        session.setSessionNo(request.getSessionNo() == null || request.getSessionNo().isBlank()
            ? UUID.randomUUID().toString()
            : request.getSessionNo());
        session.setPositionId(request.getPositionId());
        session.setUserId(request.getUserId() == null || request.getUserId().isBlank() ? "anonymous" : request.getUserId());
        session.setCurrentScene(ChatScene.SEARCH);
        session.setStatus(ChatSessionStatus.ACTIVE);
        return chatSessionRepository.save(session);
    }

    private void bindPositionContext(ChatSession session, ChatSessionState state, AgentExecuteRequest request) {
        String effectivePositionId = normalizeText(hasText(request.getPositionId()) ? request.getPositionId() : session.getPositionId());
        if (!hasText(effectivePositionId)) {
            state.setPositionId(null);
            session.setPositionId(null);
            return;
        }

        PositionJD positionJD = resolvePositionJd(effectivePositionId)
            .orElseThrow(() -> new IllegalArgumentException("Position JD not found: " + effectivePositionId));

        state.setPositionId(positionJD.getId());
        session.setPositionId(positionJD.getId());
        state.setFilter(mergePositionFilter(state.getFilter(), positionJdSearchFilterResolver.resolve(positionJD)));
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

    private void applySelection(ChatSessionState state, AgentRouteDecision routeDecision, String userInput) {
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
        state.setCurrentScene(routeDecision.getScene());
        state.setCurrentQuery(resolveCurrentQuery(state.getCurrentQuery(), routeDecision.getScene(), userInput));

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

        if (executionResult.getSearchResponse() != null && executionResult.getSearchResponse().getCandidates() != null) {
            state.setLastCandidateIds(executionResult.getSearchResponse().getCandidates().stream()
                .map(candidate -> candidate.getCandidateId())
                .toList());
            state.setSelectedCandidateIds(null);
        }
    }

    private String buildSummary(AgentToolExecutionResult executionResult, AgentRouteDecision routeDecision) {
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
            throw new IllegalStateException("Failed to serialize chat message payload.", ex);
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
        if (scene == ChatScene.FILTER_REFINE) {
            return mergeQuery(currentQuery, userInput);
        }
        if (scene == ChatScene.COMPARE || scene == ChatScene.INTERVIEW) {
            return currentQuery == null || currentQuery.isBlank() ? userInput : currentQuery;
        }
        return userInput;
    }

    private CandidateSearchFilter mergePositionFilter(CandidateSearchFilter sessionFilter, CandidateSearchFilter positionFilter) {
        CandidateSearchFilter merged = sessionFilter == null ? new CandidateSearchFilter() : sessionFilter;
        if (positionFilter == null) {
            return merged;
        }

        if (merged.getCareerStage() == null) {
            merged.setCareerStage(positionFilter.getCareerStage());
        }
        if ((merged.getHighestDegrees() == null || merged.getHighestDegrees().isEmpty())
            && positionFilter.getHighestDegrees() != null && !positionFilter.getHighestDegrees().isEmpty()) {
            merged.setHighestDegrees(positionFilter.getHighestDegrees());
        }
        if (merged.getMinYearsOfExperience() == null) {
            merged.setMinYearsOfExperience(positionFilter.getMinYearsOfExperience());
        } else if (positionFilter.getMinYearsOfExperience() != null) {
            merged.setMinYearsOfExperience(max(merged.getMinYearsOfExperience(), positionFilter.getMinYearsOfExperience()));
        }
        if ((merged.getCurrentCity() == null || merged.getCurrentCity().isBlank())
            && positionFilter.getCurrentCity() != null && !positionFilter.getCurrentCity().isBlank()) {
            merged.setCurrentCity(positionFilter.getCurrentCity());
        }
        return merged;
    }

    private BigDecimal max(BigDecimal left, BigDecimal right) {
        return left.compareTo(right) >= 0 ? left : right;
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private String normalizeText(String text) {
        return text == null ? null : text.trim();
    }

    private java.util.Optional<PositionJD> resolvePositionJd(String positionIdOrJdNo) {
        return positionJDRepository.findById(positionIdOrJdNo)
            .or(() -> positionJDRepository.findByJdNo(positionIdOrJdNo));
    }
}
