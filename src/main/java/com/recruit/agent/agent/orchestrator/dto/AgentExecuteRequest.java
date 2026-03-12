package com.recruit.agent.agent.orchestrator.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Agent 执行请求。
 */
@Getter
@Setter
@NoArgsConstructor
public class AgentExecuteRequest {

    /**
     * 会话编号。
     */
    private String sessionNo;

    /**
     * 用户编号。
     */
    private String userId;

    /**
     * 用户输入。
     */
    private String userInput;
}
