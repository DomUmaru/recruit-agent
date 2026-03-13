package com.recruit.agent.agent.router;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.agent.router.dto.AgentRoutingContext;
import com.recruit.agent.agent.router.impl.AgentRouterServiceImpl;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.llm.LlmGenerationService;
import org.junit.jupiter.api.Test;

class AgentRouterLlmRoutingTest {

    @Test
    void shouldUseLlmDecisionWhenAvailable() {
        AgentRouterServiceImpl routerService = new AgentRouterServiceImpl(
            new StubLlmGenerationService(true, "{\"scene\":\"COMPARE\",\"reason\":\"user asks to compare shortlisted candidates\"}"),
            new ObjectMapper()
        );

        AgentRoutingContext context = new AgentRoutingContext();
        context.setCurrentScene(ChatScene.SEARCH);
        context.setCurrentQuery("Java 搜索工程师");
        context.setLastCandidateIdsJson("[\"candidate-1\",\"candidate-2\"]");
        context.setUserInput("帮我比较前两个人");

        AgentRouteDecision decision = routerService.route(context);

        assertEquals(ChatScene.COMPARE, decision.getScene());
        assertEquals("compareCandidatesTool", decision.getToolName());
        assertEquals("llm-router: user asks to compare shortlisted candidates", decision.getReason());
    }

    @Test
    void shouldFallBackToRulesWhenLlmSuggestsHistoryDependentSceneWithoutHistory() {
        AgentRouterServiceImpl routerService = new AgentRouterServiceImpl(
            new StubLlmGenerationService(true, "{\"scene\":\"COMPARE\",\"reason\":\"user asks to compare shortlisted candidates\"}"),
            new ObjectMapper()
        );

        AgentRoutingContext context = new AgentRoutingContext();
        context.setUserInput("帮我比较一下");

        AgentRouteDecision decision = routerService.route(context);

        assertEquals(ChatScene.SEARCH, decision.getScene());
        assertEquals("searchCandidateByJDTool", decision.getToolName());
        assertFalse(decision.isHistoryRequired());
    }

    private static class StubLlmGenerationService implements LlmGenerationService {

        private final boolean available;
        private final String response;

        private StubLlmGenerationService(boolean available, String response) {
            this.available = available;
            this.response = response;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public String generate(String systemPrompt, String userPrompt) {
            return response;
        }
    }
}
