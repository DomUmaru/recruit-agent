package com.recruit.agent.llm;

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
