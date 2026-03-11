package com.recruit.agent.resume.parser;

/**
 * 简历文本清洗器。
 */
public interface ResumeTextCleaner {

    /**
     * 清洗提取出的原始文本。
     *
     * @param rawText 原始文本
     * @return 清洗后的文本
     */
    String clean(String rawText);
}
