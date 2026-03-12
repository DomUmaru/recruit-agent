package com.recruit.agent.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient 配置。
 */
@Configuration
public class ChatClientConfig {

    /**
     * 构建招聘 Agent 使用的 ChatClient。
     *
     * @param chatModel 聊天模型
     * @return ChatClient
     */
    @ConditionalOnBean(ChatModel.class)
    @Bean("recruitAgentChatClient")
    ChatClient recruitAgentChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
