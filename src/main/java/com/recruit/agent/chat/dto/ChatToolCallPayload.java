package com.recruit.agent.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * tool_call 事件载荷。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatToolCallPayload {

    private String toolName;

    private String reason;
}
