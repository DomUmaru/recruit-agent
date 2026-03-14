package com.recruit.agent.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Unified chat request.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatRequest {

    /**
     * Bound position ID for the current chat session.
     */
    private String positionId;

    /**
     * Session identifier. A new session will be created when absent.
     */
    private String sessionNo;

    /**
     * User identifier. Falls back to anonymous when absent.
     */
    private String userId;

    /**
     * User input text.
     */
    @NotBlank
    private String message;
}
