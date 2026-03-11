package com.recruit.agent.resume.parser;

import com.recruit.agent.resume.parser.ocr.OcrEngine;
import com.recruit.agent.resume.model.ResumeParseType;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * OCR 扫描件解析器占位实现。
 */
@Component
public class OcrResumeParser implements ResumeParser {

    private final OcrEngine ocrEngine;
    private final ResumeTextCleaner resumeTextCleaner;

    public OcrResumeParser(OcrEngine ocrEngine, ResumeTextCleaner resumeTextCleaner) {
        this.ocrEngine = ocrEngine;
        this.resumeTextCleaner = resumeTextCleaner;
    }

    @Override
    public boolean supports(Path filePath) {
        return filePath.toString().toLowerCase().endsWith(".pdf");
    }

    @Override
    public ResumeParseResult parse(Path filePath) throws IOException {
        ResumeParseResult result = new ResumeParseResult();
        result.setParseType(ResumeParseType.OCR_SCANNED);
        if (!ocrEngine.isAvailable()) {
            result.setRawText("");
            result.setCleanedText("");
            result.setPageTexts(List.of());
            result.setPageCount(0);
            result.setTextExtractable(false);
            result.setParserLog("OCR 引擎不可用，当前仅保留 OCR fallback 接口");
            return result;
        }

        List<String> pageTexts = ocrEngine.recognize(filePath);
        String rawText = String.join("\n\n", pageTexts);
        result.setRawText(rawText);
        result.setCleanedText(resumeTextCleaner.clean(rawText));
        result.setPageTexts(pageTexts);
        result.setPageCount(pageTexts.size());
        result.setTextExtractable(!result.getCleanedText().isBlank());
        result.setParserLog("OCR 解析完成");
        return result;
    }
}
