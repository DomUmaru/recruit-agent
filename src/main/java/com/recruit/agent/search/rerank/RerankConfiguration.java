package com.recruit.agent.search.rerank;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RerankProviderProperties.class)
public class RerankConfiguration {
}
