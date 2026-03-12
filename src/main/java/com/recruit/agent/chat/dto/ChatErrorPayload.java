package com.recruit.agent.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * error 事件载荷。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatErrorPayload {

    private String code;

    private String message;

    private boolean retryable;
}
