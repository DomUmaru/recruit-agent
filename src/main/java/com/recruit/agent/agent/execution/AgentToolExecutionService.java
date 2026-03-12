package com.recruit.agent.agent.execution;

import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.chat.state.ChatSessionState;
import com.recruit.agent.search.vo.CandidateSearchResponse;

/**
 * Agent 工具执行服务。
 */
public interface AgentToolExecutionService {

    /**
     * 根据路由决策执行对应工具。
     *
     * @param routeDecision 路由决策
     * @param state 会话状态
     * @param userInput 用户输入
     * @return 工具执行结果
     */
    CandidateSearchResponse execute(AgentRouteDecision routeDecision, ChatSessionState state, String userInput);
}
