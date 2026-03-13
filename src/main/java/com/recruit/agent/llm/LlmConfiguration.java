package com.recruit.agent.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableConfigurationProperties(LlmProviderProperties.class)
public class LlmConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LlmConfiguration.class);

    @Bean
    @ConditionalOnBean(name = "recruitAgentChatClient")
    @ConditionalOnExpression("'${app.llm.provider:none}' != 'none'")
    LlmGenerationService springAiLlmGenerationService(@Qualifier("recruitAgentChatClient") ChatClient recruitAgentChatClient) {
        log.info("Registering Spring AI LLM generation service for provider={}", "configured-openai-compatible");
        return new SpringAiLlmGenerationService(recruitAgentChatClient);
    }

    @Bean
    @ConditionalOnMissingBean(LlmGenerationService.class)
    LlmGenerationService unavailableLlmGenerationService() {
        log.warn("Falling back to unavailable LLM generation service. LLM summaries will use deterministic fallback.");
        return new UnavailableLlmGenerationService();
    }
}
