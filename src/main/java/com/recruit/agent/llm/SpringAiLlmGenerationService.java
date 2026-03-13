package com.recruit.agent.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(name = "recruitAgentChatClient")
@ConditionalOnProperty(prefix = "app.llm", name = "provider", havingValue = "qwen")
public class SpringAiLlmGenerationService implements LlmGenerationService {

    private final ChatClient recruitAgentChatClient;

    public SpringAiLlmGenerationService(@Qualifier("recruitAgentChatClient") ChatClient recruitAgentChatClient) {
        this.recruitAgentChatClient = recruitAgentChatClient;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        return recruitAgentChatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .call()
            .content();
    }
}
