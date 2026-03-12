package com.recruit.agent.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * token 事件载荷。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatTokenPayload {

    private int index;

    private String content;
}
