package com.recruit.agent.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class LlmStartupDiagnostics implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LlmStartupDiagnostics.class);

    private final Environment environment;
    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ObjectProvider<ChatClient> recruitAgentChatClientProvider;
    private final LlmGenerationService llmGenerationService;

    public LlmStartupDiagnostics(Environment environment,
                                 ObjectProvider<ChatModel> chatModelProvider,
                                 @Qualifier("recruitAgentChatClient") ObjectProvider<ChatClient> recruitAgentChatClientProvider,
                                 LlmGenerationService llmGenerationService) {
        this.environment = environment;
        this.chatModelProvider = chatModelProvider;
        this.recruitAgentChatClientProvider = recruitAgentChatClientProvider;
        this.llmGenerationService = llmGenerationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        ChatClient recruitAgentChatClient = recruitAgentChatClientProvider.getIfAvailable();
        log.info(
            "LLM diagnostics: spring.ai.model.chat={}, app.llm.provider={}, chatModelPresent={}, recruitAgentChatClientPresent={}, llmService={}",
            environment.getProperty("spring.ai.model.chat"),
            environment.getProperty("app.llm.provider"),
            chatModel != null,
            recruitAgentChatClient != null,
            llmGenerationService.getClass().getSimpleName()
        );
    }
}
