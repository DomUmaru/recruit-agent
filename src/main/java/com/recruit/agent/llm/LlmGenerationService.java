package com.recruit.agent.llm;

public interface LlmGenerationService {

    boolean isAvailable();

    String generate(String systemPrompt, String userPrompt);
}
