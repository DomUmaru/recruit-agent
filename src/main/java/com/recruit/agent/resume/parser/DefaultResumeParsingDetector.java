package com.recruit.agent.resume.parser;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 默认简历解析策略判定器。
 */
@Component
public class DefaultResumeParsingDetector implements ResumeParsingDetector {

    private static final int MIN_TOTAL_TEXT_LENGTH = 80;
    private static final int MIN_NON_EMPTY_PAGES = 1;

    @Value("${app.resume.force-ocr:false}")
    private boolean forceOcr;

    @Override
    public ResumeParsingDecision detect(ResumeParseResult parseResult) {
        ResumeParsingDecision decision = new ResumeParsingDecision();

        if (forceOcr) {
            decision.setTextPdf(false);
            decision.setRequireOcrFallback(true);
            decision.setReason("本地调试已启用 force-ocr，强制走 OCR 解析链路");
            return decision;
        }

        int totalTextLength = safeText(parseResult.getCleanedText()).length();
        int nonEmptyPages = countNonEmptyPages(parseResult.getPageTexts());

        boolean textPdf = parseResult.isTextExtractable()
            && totalTextLength >= MIN_TOTAL_TEXT_LENGTH
            && nonEmptyPages >= MIN_NON_EMPTY_PAGES;

        decision.setTextPdf(textPdf);
        decision.setRequireOcrFallback(!textPdf);

        if (textPdf) {
            decision.setReason("检测到可直接提取的文本内容，走 PDFBox 文本解析路径");
        } else {
            decision.setReason("提取文本不足，疑似扫描件 PDF，需要 OCR 降级");
        }

        return decision;
    }

    private int countNonEmptyPages(List<String> pageTexts) {
        if (pageTexts == null || pageTexts.isEmpty()) {
            return 0;
        }
        return (int) pageTexts.stream()
            .filter(text -> text != null && !text.isBlank())
            .count();
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }
}
