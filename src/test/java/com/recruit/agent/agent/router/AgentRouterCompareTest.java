package com.recruit.agent.agent.router;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.agent.router.dto.AgentRoutingContext;
import com.recruit.agent.agent.router.impl.AgentRouterServiceImpl;
import com.recruit.agent.chat.model.ChatScene;
import org.junit.jupiter.api.Test;

class AgentRouterCompareTest {

    @Test
    void shouldRouteToCompareWhenHistoryExistsAndInputContainsCompareIntent() {
        AgentRouterServiceImpl routerService = new AgentRouterServiceImpl();

        AgentRoutingContext context = new AgentRoutingContext();
        context.setCurrentScene(ChatScene.SEARCH);
        context.setCurrentQuery("推荐系统");
        context.setLastCandidateIdsJson("[\"candidate-1\",\"candidate-2\"]");
        context.setUserInput("把前2个对比一下");

        AgentRouteDecision decision = routerService.route(context);

        assertEquals(ChatScene.COMPARE, decision.getScene());
        assertEquals("compareCandidatesTool", decision.getToolName());
        assertTrue(decision.isHistoryRequired());
    }
}
