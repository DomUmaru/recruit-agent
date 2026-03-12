package com.recruit.agent.chat.service;

import com.recruit.agent.chat.dto.ChatRequest;
import com.recruit.agent.chat.dto.ChatResponse;
import com.recruit.agent.chat.dto.ChatStreamEvent;
import java.util.List;

/**
 * 聊天应用服务。
 */
public interface ChatApplicationService {

    /**
     * 执行一次聊天请求。
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 执行一次聊天请求并返回 SSE 事件序列。
     *
     * @param request 聊天请求
     * @return 事件列表
     */
    List<ChatStreamEvent> stream(ChatRequest request);
}
