package com.recruit.agent.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruit.agent.agent.orchestrator.AgentOrchestratorService;
import com.recruit.agent.agent.orchestrator.dto.AgentExecuteResponse;
import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.chat.dto.ChatRequest;
import com.recruit.agent.chat.dto.ChatResponse;
import com.recruit.agent.chat.dto.ChatStreamEvent;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.chat.service.impl.ChatApplicationServiceImpl;
import com.recruit.agent.comparison.vo.CandidateComparisonEvidenceVO;
import com.recruit.agent.comparison.vo.CandidateComparisonItemVO;
import com.recruit.agent.comparison.vo.CandidateComparisonResponse;
import com.recruit.agent.interview.vo.CandidateInterviewQuestionVO;
import com.recruit.agent.interview.vo.InterviewQuestionItemVO;
import com.recruit.agent.interview.vo.InterviewQuestionResponse;
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
        candidate.setRank(1);
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
        executeResponse.setSummary("已完成候选人搜索，返回 1 位候选人。");

        when(orchestratorService.execute(any())).thenReturn(executeResponse);

        ChatRequest request = new ChatRequest();
        request.setPositionId("position-1");
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
        verify(orchestratorService, org.mockito.Mockito.atLeastOnce()).execute(org.mockito.ArgumentMatchers.argThat(
            executeRequest -> "position-1".equals(executeRequest.getPositionId())
        ));
    }

    @Test
    void shouldBuildComparisonPayloadAndEvents() {
        AgentOrchestratorService orchestratorService = org.mockito.Mockito.mock(AgentOrchestratorService.class);
        ChatApplicationServiceImpl service = new ChatApplicationServiceImpl(orchestratorService);

        AgentRouteDecision decision = new AgentRouteDecision();
        decision.setScene(ChatScene.COMPARE);
        decision.setToolName("compareCandidatesTool");

        CandidateComparisonEvidenceVO evidence = new CandidateComparisonEvidenceVO();
        evidence.setSection("project");
        evidence.setPage(1);
        evidence.setContent("负责推荐系统召回优化");

        CandidateComparisonItemVO candidate = new CandidateComparisonItemVO();
        candidate.setRank(1);
        candidate.setCandidateId("candidate-1");
        candidate.setCandidateNo("C-001");
        candidate.setFullName("张三");
        candidate.setHighlights(List.of("技术栈覆盖: Java, Elasticsearch"));
        candidate.setRiskPoints(List.of("存在外包/驻场标签"));
        candidate.setEvidenceList(List.of(evidence));

        CandidateComparisonResponse comparisonResponse = new CandidateComparisonResponse();
        comparisonResponse.setTargetQuery("推荐系统");
        comparisonResponse.setCandidates(List.of(candidate));
        comparisonResponse.setSummary("张三在推荐系统相关背景上更突出。");

        AgentExecuteResponse executeResponse = new AgentExecuteResponse();
        executeResponse.setSessionNo("session-2");
        executeResponse.setRouteDecision(decision);
        executeResponse.setComparisonResponse(comparisonResponse);
        executeResponse.setSummary("张三在推荐系统相关背景上更突出。");

        when(orchestratorService.execute(any())).thenReturn(executeResponse);

        ChatRequest request = new ChatRequest();
        request.setSessionNo("session-2");
        request.setUserId("user-1");
        request.setMessage("对比一下这两个人");

        ChatResponse response = service.chat(request);
        List<ChatStreamEvent> events = service.stream(request);

        assertEquals(ChatScene.COMPARE, response.getScene());
        assertEquals(1, response.getComparison().getCandidates().get(0).getRank());
        assertEquals("推荐系统", response.getComparison().getTargetQuery());
        assertEquals("comparison", events.get(4).getEvent());
        assertEquals("state_update", events.get(5).getEvent());
        assertFalse(events.stream().anyMatch(event -> "citation".equals(event.getEvent())));
        assertTrue(events.stream().anyMatch(event -> "token".equals(event.getEvent())));
    }

    @Test
    void shouldBuildInterviewEvents() {
        AgentOrchestratorService orchestratorService = org.mockito.Mockito.mock(AgentOrchestratorService.class);
        ChatApplicationServiceImpl service = new ChatApplicationServiceImpl(orchestratorService);

        AgentRouteDecision decision = new AgentRouteDecision();
        decision.setScene(ChatScene.INTERVIEW);
        decision.setToolName("generateInterviewQuestionsTool");

        InterviewQuestionItemVO question = new InterviewQuestionItemVO();
        question.setCategory("技术深挖");
        question.setQuestion("请介绍你做推荐系统召回优化时的关键技术取舍。");

        CandidateInterviewQuestionVO candidate = new CandidateInterviewQuestionVO();
        candidate.setRank(1);
        candidate.setCandidateId("candidate-1");
        candidate.setCandidateNo("C-001");
        candidate.setFullName("张三");
        candidate.setQuestions(List.of(question));

        InterviewQuestionResponse interviewResponse = new InterviewQuestionResponse();
        interviewResponse.setTargetQuery("推荐系统");
        interviewResponse.setCandidates(List.of(candidate));
        interviewResponse.setSummary("已围绕推荐系统生成候选人面试题。");

        AgentExecuteResponse executeResponse = new AgentExecuteResponse();
        executeResponse.setSessionNo("session-3");
        executeResponse.setRouteDecision(decision);
        executeResponse.setInterviewResponse(interviewResponse);
        executeResponse.setSummary("已围绕推荐系统生成候选人面试题。");

        when(orchestratorService.execute(any())).thenReturn(executeResponse);

        ChatRequest request = new ChatRequest();
        request.setSessionNo("session-3");
        request.setUserId("user-1");
        request.setMessage("给这两个人出面试题");

        ChatResponse response = service.chat(request);
        List<ChatStreamEvent> events = service.stream(request);

        assertEquals(ChatScene.INTERVIEW, response.getScene());
        assertEquals(1, response.getInterviewResponse().getCandidates().get(0).getRank());
        assertEquals("推荐系统", response.getInterviewResponse().getTargetQuery());
        assertEquals("interview", events.get(4).getEvent());
        assertEquals("state_update", events.get(5).getEvent());
        assertFalse(events.stream().anyMatch(event -> "citation".equals(event.getEvent())));
        assertTrue(events.stream().anyMatch(event -> "token".equals(event.getEvent())));
    }
}
