package com.recruit.agent.resume.parser;

import com.recruit.agent.resume.model.ResumeParseType;
import java.util.List;

/**
 * 简历解析结果对象。
 */
public class ResumeParseResult {

    /**
     * 解析类型。
     */
    private ResumeParseType parseType;

    /**
     * 原始提取文本。
     */
    private String rawText;

    /**
     * 清洗后的文本。
     */
    private String cleanedText;

    /**
     * 按页文本列表。
     */
    private List<String> pageTexts;

    /**
     * 文档总页数。
     */
    private Integer pageCount;

    /**
     * 是否检测到可直接提取的文本。
     */
    private boolean textExtractable;

    /**
     * 解析过程说明。
     */
    private String parserLog;

    public ResumeParseType getParseType() {
        return parseType;
    }

    public void setParseType(ResumeParseType parseType) {
        this.parseType = parseType;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getCleanedText() {
        return cleanedText;
    }

    public void setCleanedText(String cleanedText) {
        this.cleanedText = cleanedText;
    }

    public List<String> getPageTexts() {
        return pageTexts;
    }

    public void setPageTexts(List<String> pageTexts) {
        this.pageTexts = pageTexts;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public boolean isTextExtractable() {
        return textExtractable;
    }

    public void setTextExtractable(boolean textExtractable) {
        this.textExtractable = textExtractable;
    }

    public String getParserLog() {
        return parserLog;
    }

    public void setParserLog(String parserLog) {
        this.parserLog = parserLog;
    }
}
