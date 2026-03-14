package com.recruit.agent.chat.dto;

import com.recruit.agent.chat.model.ChatScene;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * router_decision 事件载荷。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatRouterDecisionPayload {

    private ChatScene scene;

    private String toolName;

    private boolean historyRequired;

    private String reason;
}
