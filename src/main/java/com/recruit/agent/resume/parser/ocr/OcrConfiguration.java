package com.recruit.agent.resume.parser.ocr;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OcrProviderProperties.class)
public class OcrConfiguration {
}
