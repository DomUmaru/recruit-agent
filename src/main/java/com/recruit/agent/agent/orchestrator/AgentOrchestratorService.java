package com.recruit.agent.agent.orchestrator;

import com.recruit.agent.agent.orchestrator.dto.AgentExecuteRequest;
import com.recruit.agent.agent.orchestrator.dto.AgentExecuteResponse;

/**
 * Agent 编排服务。
 */
public interface AgentOrchestratorService {

    /**
     * 执行一次 Agent 请求。
     *
     * @param request 执行请求
     * @return 执行结果
     */
    AgentExecuteResponse execute(AgentExecuteRequest request);
}
