package com.recruit.agent.resume.parser;

/**
 * 简历解析决策结果。
 */
public class ResumeParsingDecision {

    /**
     * 是否判定为文本型 PDF。
     */
    private boolean textPdf;

    /**
     * 是否需要 OCR 降级。
     */
    private boolean requireOcrFallback;

    /**
     * 判定说明。
     */
    private String reason;

    public boolean isTextPdf() {
        return textPdf;
    }

    public void setTextPdf(boolean textPdf) {
        this.textPdf = textPdf;
    }

    public boolean isRequireOcrFallback() {
        return requireOcrFallback;
    }

    public void setRequireOcrFallback(boolean requireOcrFallback) {
        this.requireOcrFallback = requireOcrFallback;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
