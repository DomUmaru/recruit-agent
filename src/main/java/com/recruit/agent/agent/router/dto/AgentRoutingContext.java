package com.recruit.agent.agent.router.dto;

import com.recruit.agent.chat.model.ChatScene;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Agent 路由上下文。
 */
@Getter
@Setter
@NoArgsConstructor
public class AgentRoutingContext {

    /**
     * 用户当前输入。
     */
    private String userInput;

    /**
     * 会话当前场景。
     */
    private ChatScene currentScene;

    /**
     * 会话中保留的当前查询。
     */
    private String currentQuery;

    /**
     * 会话中保留的筛选条件 JSON。
     */
    private String filtersJson;

    /**
     * 上一轮候选人结果 ID JSON。
     */
    private String lastCandidateIdsJson;
}
