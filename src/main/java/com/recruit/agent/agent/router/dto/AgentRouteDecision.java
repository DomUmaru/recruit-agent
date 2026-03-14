package com.recruit.agent.agent.router.dto;

import com.recruit.agent.chat.model.ChatScene;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Agent 路由决策结果。
 */
@Getter
@Setter
@NoArgsConstructor
public class AgentRouteDecision {

    /**
     * 路由目标场景。
     */
    private ChatScene scene;

    /**
     * 推荐调用的工具名称。
     */
    private String toolName;

    /**
     * 当前决策是否依赖历史状态。
     */
    private boolean historyRequired;

    /**
     * 决策原因。
     */
    private String reason;
}
