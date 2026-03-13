package com.recruit.agent.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.llm", name = "provider", havingValue = "none", matchIfMissing = true)
public class UnavailableLlmGenerationService implements LlmGenerationService {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        throw new IllegalStateException("LLM provider is not configured");
    }
}
