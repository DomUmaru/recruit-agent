package com.recruit.agent.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.recruit.agent.agent.orchestrator.AgentOrchestratorService;
import com.recruit.agent.agent.orchestrator.dto.AgentExecuteResponse;
import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.chat.dto.ChatRequest;
import com.recruit.agent.chat.dto.ChatResponse;
import com.recruit.agent.chat.dto.ChatStreamEvent;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.chat.service.impl.ChatApplicationServiceImpl;
import com.recruit.agent.search.vo.CandidateSearchEvidenceVO;
import com.recruit.agent.search.vo.CandidateSearchItemVO;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatApplicationServiceImplTest {

    @Test
    void shouldBuildChatResponseAndStreamEvents() {
        AgentOrchestratorService orchestratorService = org.mockito.Mockito.mock(AgentOrchestratorService.class);
        ChatApplicationServiceImpl service = new ChatApplicationServiceImpl(orchestratorService);

        AgentRouteDecision decision = new AgentRouteDecision();
        decision.setScene(ChatScene.SEARCH);
        decision.setToolName("searchCandidateByJDTool");

        CandidateSearchEvidenceVO evidence = new CandidateSearchEvidenceVO();
        evidence.setChunkId("chunk-1");
        evidence.setDocId("doc-1");
        evidence.setSection("project");
        evidence.setPage(1);
        evidence.setContent("负责推荐系统召回。");

        CandidateSearchItemVO candidate = new CandidateSearchItemVO();
        candidate.setCandidateId("candidate-1");
        candidate.setCandidateNo("C-001");
        candidate.setFullName("张三");
        candidate.setEvidenceList(List.of(evidence));

        CandidateSearchResponse searchResponse = new CandidateSearchResponse();
        searchResponse.setQuery("Java");
        searchResponse.setTotal(1);
        searchResponse.setCandidates(List.of(candidate));

        AgentExecuteResponse executeResponse = new AgentExecuteResponse();
        executeResponse.setSessionNo("session-1");
        executeResponse.setRouteDecision(decision);
        executeResponse.setSearchResponse(searchResponse);
        executeResponse.setSummary("已完成候选人搜索，返回 2 位候选人。");

        when(orchestratorService.execute(any())).thenReturn(executeResponse);

        ChatRequest request = new ChatRequest();
        request.setSessionNo("session-1");
        request.setUserId("user-1");
        request.setMessage("找 Java 候选人");

        ChatResponse response = service.chat(request);
        List<ChatStreamEvent> events = service.stream(request);

        assertEquals("session-1", response.getSessionNo());
        assertEquals(ChatScene.SEARCH, response.getScene());
        assertEquals("start", events.get(0).getEvent());
        assertEquals("router_decision", events.get(1).getEvent());
        assertEquals("tool_call", events.get(2).getEvent());
        assertEquals("tool_result", events.get(3).getEvent());
        assertEquals("state_update", events.get(4).getEvent());
        assertEquals("citation", events.get(5).getEvent());
        assertEquals("done", events.get(events.size() - 1).getEvent());
        assertTrue(events.stream().anyMatch(event -> "token".equals(event.getEvent())));
        assertFalse(response.getSummary().isBlank());
    }
}
