package com.recruit.agent.agent.orchestrator.dto;

import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Agent 执行响应。
 */
@Getter
@Setter
@NoArgsConstructor
public class AgentExecuteResponse {

    /**
     * 会话编号。
     */
    private String sessionNo;

    /**
     * 路由决策。
     */
    private AgentRouteDecision routeDecision;

    /**
     * 搜索结果。
     */
    private CandidateSearchResponse searchResponse;

    /**
     * 面向上层的简短摘要。
     */
    private String summary;
}
