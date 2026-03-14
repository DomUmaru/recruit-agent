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
        context.setUserInput("\u627e\u505a\u63a8\u8350\u7cfb\u7edf\u7684\u5019\u9009\u4eba");

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
        context.setCurrentQuery("\u63a8\u8350\u7cfb\u7edf");
        context.setLastCandidateIdsJson("[\"candidate-1\",\"candidate-2\"]");
        context.setUserInput("\u53ea\u770b985\u7855\u58eb\uff0c\u518d\u52a03\u5e74\u4ee5\u4e0a\u7ecf\u9a8c");

        AgentRouteDecision decision = routerService.route(context);

        assertEquals(ChatScene.FILTER_REFINE, decision.getScene());
        assertEquals("refineSearchFilterTool", decision.getToolName());
        assertTrue(decision.isHistoryRequired());
    }

    @Test
    void shouldRouteToRefineWhenUserSaysContinueFiltering() {
        AgentRouterServiceImpl routerService = new AgentRouterServiceImpl();

        AgentRoutingContext context = new AgentRoutingContext();
        context.setCurrentScene(ChatScene.FILTER_REFINE);
        context.setCurrentQuery("Java \u540e\u7aef");
        context.setLastCandidateIdsJson("[\"candidate-1\",\"candidate-2\"]");
        context.setUserInput("\u7ee7\u7eed\u53ea\u770b\u5317\u4eac\u7684");

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
        context.setCurrentQuery("Java \u540e\u7aef");
        context.setLastCandidateIdsJson("[\"candidate-1\"]");
        context.setUserInput("\u627e\u505a\u5e7f\u544a\u7cfb\u7edf\u7684\u5019\u9009\u4eba");

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
        context.setCurrentQuery("\u63a8\u8350\u7cfb\u7edf");
        context.setLastCandidateIdsJson("[\"candidate-1\",\"candidate-2\"]");
        context.setUserInput("\u7ed9\u8fd9\u4e24\u4e2a\u4eba\u751f\u6210\u4e00\u4efd\u9762\u8bd5\u4ea4\u63a5\u63d0\u7eb2");

        AgentRouteDecision decision = routerService.route(context);

        assertEquals(ChatScene.INTERVIEW, decision.getScene());
        assertEquals("generateInterviewQuestionsTool", decision.getToolName());
        assertTrue(decision.isHistoryRequired());
    }
}
