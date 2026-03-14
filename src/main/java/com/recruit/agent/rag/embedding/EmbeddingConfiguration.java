package com.recruit.agent.rag.embedding;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EmbeddingProviderProperties.class)
public class EmbeddingConfiguration {
}
