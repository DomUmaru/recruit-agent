package com.recruit.agent.chat.state;

import com.recruit.agent.chat.model.ChatSession;

/**
 * 会话状态装配服务。
 */
public interface ChatSessionStateService {

    /**
     * 从持久化会话中加载运行态对象。
     *
     * @param session 会话实体
     * @return 运行态对象
     */
    ChatSessionState load(ChatSession session);

    /**
     * 将运行态对象回写到会话实体。
     *
     * @param session 会话实体
     * @param state 运行态对象
     */
    void apply(ChatSession session, ChatSessionState state);
}
