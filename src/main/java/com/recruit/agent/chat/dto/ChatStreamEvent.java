package com.recruit.agent.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SSE 聊天事件。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatStreamEvent {

    private String event;

    private Object data;
}
