package com.recruit.agent.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * start 事件载荷。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatSessionPayload {

    private String sessionNo;
}
