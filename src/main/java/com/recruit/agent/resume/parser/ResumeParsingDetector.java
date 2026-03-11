package com.recruit.agent.resume.parser;

/**
 * 简历解析策略判定器。
 */
public interface ResumeParsingDetector {

    /**
     * 基于初步解析结果判断是否需要 OCR 降级。
     *
     * @param parseResult PDFBox 初步解析结果
     * @return 判定结果
     */
    ResumeParsingDecision detect(ResumeParseResult parseResult);
}
