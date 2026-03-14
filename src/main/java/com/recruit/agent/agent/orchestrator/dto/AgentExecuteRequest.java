package com.recruit.agent.agent.orchestrator.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Internal agent execute request.
 */
@Getter
@Setter
@NoArgsConstructor
public class AgentExecuteRequest {

    /**
     * Bound position ID for the current execution.
     */
    private String positionId;

    /**
     * Session identifier.
     */
    private String sessionNo;

    /**
     * User identifier.
     */
    private String userId;

    /**
     * User input text.
     */
    private String userInput;
}
