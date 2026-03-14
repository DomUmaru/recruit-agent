package com.recruit.agent.agent.router;

import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.agent.router.dto.AgentRoutingContext;

/**
 * Agent 路由服务。
 */
public interface AgentRouterService {

    /**
     * 根据当前输入与会话状态进行路由决策。
     *
     * @param context 路由上下文
     * @return 路由结果
     */
    AgentRouteDecision route(AgentRoutingContext context);
}
