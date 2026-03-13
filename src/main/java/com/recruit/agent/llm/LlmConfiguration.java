package com.recruit.agent.llm;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LlmProviderProperties.class)
public class LlmConfiguration {
}
