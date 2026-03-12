package com.recruit.agent.chat.service.impl;

import com.recruit.agent.agent.orchestrator.AgentOrchestratorService;
import com.recruit.agent.agent.orchestrator.dto.AgentExecuteRequest;
import com.recruit.agent.agent.orchestrator.dto.AgentExecuteResponse;
import com.recruit.agent.chat.dto.ChatCitationPayload;
import com.recruit.agent.chat.dto.ChatRequest;
import com.recruit.agent.chat.dto.ChatResponse;
import com.recruit.agent.chat.dto.ChatRouterDecisionPayload;
import com.recruit.agent.chat.dto.ChatSessionPayload;
import com.recruit.agent.chat.dto.ChatStateUpdatePayload;
import com.recruit.agent.chat.dto.ChatStreamEvent;
import com.recruit.agent.chat.dto.ChatTokenPayload;
import com.recruit.agent.chat.dto.ChatToolCallPayload;
import com.recruit.agent.chat.service.ChatApplicationService;
import com.recruit.agent.search.vo.CandidateSearchEvidenceVO;
import com.recruit.agent.search.vo.CandidateSearchItemVO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 聊天应用服务实现。
 */
@Service
public class ChatApplicationServiceImpl implements ChatApplicationService {

    private final AgentOrchestratorService agentOrchestratorService;

    public ChatApplicationServiceImpl(AgentOrchestratorService agentOrchestratorService) {
        this.agentOrchestratorService = agentOrchestratorService;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        AgentExecuteResponse executeResponse = agentOrchestratorService.execute(toExecuteRequest(request));
        return toChatResponse(executeResponse);
    }

    @Override
    public List<ChatStreamEvent> stream(ChatRequest request) {
        AgentExecuteResponse executeResponse = agentOrchestratorService.execute(toExecuteRequest(request));

        List<ChatStreamEvent> events = new ArrayList<>();
        events.add(event("start", toSessionPayload(executeResponse)));
        events.add(event("router_decision", toRouterDecisionPayload(executeResponse)));
        events.add(event("tool_call", toToolCallPayload(executeResponse)));
        events.add(event("tool_result", executeResponse.getSearchResponse()));
        events.add(event("state_update", toStateUpdatePayload(executeResponse)));
        events.addAll(toCitationEvents(executeResponse));
        events.addAll(toTokenEvents(executeResponse.getSummary()));
        events.add(event("done", toChatResponse(executeResponse)));
        return events;
    }

    private AgentExecuteRequest toExecuteRequest(ChatRequest request) {
        AgentExecuteRequest executeRequest = new AgentExecuteRequest();
        executeRequest.setSessionNo(request.getSessionNo());
        executeRequest.setUserId(request.getUserId());
        executeRequest.setUserInput(request.getMessage());
        return executeRequest;
    }

    private ChatResponse toChatResponse(AgentExecuteResponse executeResponse) {
        ChatResponse response = new ChatResponse();
        response.setSessionNo(executeResponse.getSessionNo());
        response.setScene(executeResponse.getRouteDecision().getScene());
        response.setToolName(executeResponse.getRouteDecision().getToolName());
        response.setSummary(executeResponse.getSummary());
        response.setSearchResponse(executeResponse.getSearchResponse());
        return response;
    }

    private ChatStreamEvent event(String eventName, Object data) {
        ChatStreamEvent event = new ChatStreamEvent();
        event.setEvent(eventName);
        event.setData(data);
        return event;
    }

    private ChatSessionPayload toSessionPayload(AgentExecuteResponse executeResponse) {
        ChatSessionPayload payload = new ChatSessionPayload();
        payload.setSessionNo(executeResponse.getSessionNo());
        return payload;
    }

    private ChatRouterDecisionPayload toRouterDecisionPayload(AgentExecuteResponse executeResponse) {
        ChatRouterDecisionPayload payload = new ChatRouterDecisionPayload();
        payload.setScene(executeResponse.getRouteDecision().getScene());
        payload.setToolName(executeResponse.getRouteDecision().getToolName());
        payload.setHistoryRequired(executeResponse.getRouteDecision().isHistoryRequired());
        payload.setReason(executeResponse.getRouteDecision().getReason());
        return payload;
    }

    private ChatToolCallPayload toToolCallPayload(AgentExecuteResponse executeResponse) {
        ChatToolCallPayload payload = new ChatToolCallPayload();
        payload.setToolName(executeResponse.getRouteDecision().getToolName());
        payload.setReason(executeResponse.getRouteDecision().getReason());
        return payload;
    }

    private ChatStateUpdatePayload toStateUpdatePayload(AgentExecuteResponse executeResponse) {
        ChatStateUpdatePayload payload = new ChatStateUpdatePayload();
        payload.setScene(executeResponse.getRouteDecision().getScene());
        payload.setCurrentQuery(executeResponse.getSearchResponse() == null ? null : executeResponse.getSearchResponse().getQuery());
        payload.setLastCandidateIds(executeResponse.getSearchResponse() == null || executeResponse.getSearchResponse().getCandidates() == null
            ? List.of()
            : executeResponse.getSearchResponse().getCandidates().stream()
                .map(CandidateSearchItemVO::getCandidateId)
                .toList());
        return payload;
    }

    private List<ChatStreamEvent> toCitationEvents(AgentExecuteResponse executeResponse) {
        if (executeResponse.getSearchResponse() == null || executeResponse.getSearchResponse().getCandidates() == null) {
            return List.of();
        }

        List<ChatStreamEvent> events = new ArrayList<>();
        for (CandidateSearchItemVO candidate : executeResponse.getSearchResponse().getCandidates()) {
            if (candidate.getEvidenceList() == null || candidate.getEvidenceList().isEmpty()) {
                continue;
            }

            ChatCitationPayload payload = new ChatCitationPayload();
            payload.setCandidateId(candidate.getCandidateId());
            payload.setCandidateNo(candidate.getCandidateNo());
            payload.setCandidateName(candidate.getFullName());
            payload.setSnippets(candidate.getEvidenceList().stream()
                .map(this::toCitationSnippet)
                .toList());
            events.add(event("citation", payload));
        }
        return events;
    }

    private ChatCitationPayload.ChatCitationSnippet toCitationSnippet(CandidateSearchEvidenceVO evidence) {
        ChatCitationPayload.ChatCitationSnippet snippet = new ChatCitationPayload.ChatCitationSnippet();
        snippet.setChunkId(evidence.getChunkId());
        snippet.setDocId(evidence.getDocId());
        snippet.setSection(evidence.getSection());
        snippet.setPage(evidence.getPage());
        snippet.setContent(evidence.getContent());
        return snippet;
    }

    private List<ChatStreamEvent> toTokenEvents(String summary) {
        String normalized = summary == null ? "" : summary.trim();
        if (normalized.isBlank()) {
            return List.of();
        }

        List<ChatStreamEvent> events = new ArrayList<>();
        int index = 0;
        for (String token : splitSummary(normalized)) {
            ChatTokenPayload payload = new ChatTokenPayload();
            payload.setIndex(index++);
            payload.setContent(token);
            events.add(event("token", payload));
        }
        return events;
    }

    private List<String> splitSummary(String summary) {
        List<String> parts = new ArrayList<>();
        int chunkSize = 12;
        for (int start = 0; start < summary.length(); start += chunkSize) {
            int end = Math.min(summary.length(), start + chunkSize);
            parts.add(summary.substring(start, end));
        }
        return parts;
    }
}
