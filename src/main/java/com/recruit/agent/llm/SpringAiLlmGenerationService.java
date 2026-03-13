package com.recruit.agent.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
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
