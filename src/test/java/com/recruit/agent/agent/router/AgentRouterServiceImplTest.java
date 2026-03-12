package com.recruit.agent.agent.router;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.agent.router.dto.AgentRoutingContext;
import com.recruit.agent.agent.router.impl.AgentRouterServiceImpl;
import com.recruit.agent.chat.model.ChatScene;
import org.junit.jupiter.api.Test;

class AgentRouterServiceImplTest {

    @Test
    void shouldRouteToSearchWhenNoHistoryExists() {
        AgentRouterServiceImpl routerService = new AgentRouterServiceImpl();

        AgentRoutingContext context = new AgentRoutingContext();
        context.setUserInput("找做推荐系统的候选人");

        AgentRouteDecision decision = routerService.route(context);

        assertEquals(ChatScene.SEARCH, decision.getScene());
        assertEquals("searchCandidateByJDTool", decision.getToolName());
        assertFalse(decision.isHistoryRequired());
    }

    @Test
    void shouldRouteToRefineWhenHistoryExistsAndInputLooksLikeRefinement() {
        AgentRouterServiceImpl routerService = new AgentRouterServiceImpl();

        AgentRoutingContext context = new AgentRoutingContext();
        context.setCurrentScene(ChatScene.SEARCH);
        context.setCurrentQuery("推荐系统");
        context.setLastCandidateIdsJson("[\"candidate-1\",\"candidate-2\"]");
        context.setUserInput("只要985和211，再加3年以上经验");

        AgentRouteDecision decision = routerService.route(context);

        assertEquals(ChatScene.FILTER_REFINE, decision.getScene());
        assertEquals("refineSearchFilterTool", decision.getToolName());
        assertTrue(decision.isHistoryRequired());
    }

    @Test
    void shouldStillRouteToSearchWhenHistoryExistsButInputIsNewSearch() {
        AgentRouterServiceImpl routerService = new AgentRouterServiceImpl();

        AgentRoutingContext context = new AgentRoutingContext();
        context.setCurrentScene(ChatScene.SEARCH);
        context.setCurrentQuery("Java 后端");
        context.setLastCandidateIdsJson("[\"candidate-1\"]");
        context.setUserInput("找做广告系统的候选人");

        AgentRouteDecision decision = routerService.route(context);

        assertEquals(ChatScene.SEARCH, decision.getScene());
        assertEquals("searchCandidateByJDTool", decision.getToolName());
        assertFalse(decision.isHistoryRequired());
    }

    @Test
    void shouldRouteToInterviewWhenHistoryExistsAndInputLooksLikeInterviewGeneration() {
        AgentRouterServiceImpl routerService = new AgentRouterServiceImpl();

        AgentRoutingContext context = new AgentRoutingContext();
        context.setCurrentScene(ChatScene.COMPARE);
        context.setCurrentQuery("推荐系统");
        context.setLastCandidateIdsJson("[\"candidate-1\",\"candidate-2\"]");
        context.setUserInput("给这两个人出一套面试题");

        AgentRouteDecision decision = routerService.route(context);

        assertEquals(ChatScene.INTERVIEW, decision.getScene());
        assertEquals("generateInterviewQuestionsTool", decision.getToolName());
        assertTrue(decision.isHistoryRequired());
    }
}
