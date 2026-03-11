package com.recruit.agent.resume.parser;

import org.springframework.stereotype.Component;

/**
 * 默认简历文本清洗器。
 */
@Component
public class DefaultResumeTextCleaner implements ResumeTextCleaner {

    @Override
    public String clean(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String normalized = rawText
            .replace('\u0000', ' ')
            .replace("\r\n", "\n")
            .replace('\r', '\n');

        normalized = normalized.replaceAll("[\\t\\x0B\\f]+", " ");
        normalized = normalized.replaceAll("[ ]{2,}", " ");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");

        return normalized.trim();
    }
}
