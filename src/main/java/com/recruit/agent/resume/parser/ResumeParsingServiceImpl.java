package com.recruit.agent.resume.parser;

import java.io.IOException;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

/**
 * 简历解析编排服务实现。
 */
@Service
public class ResumeParsingServiceImpl implements ResumeParsingService {

    private final PdfBoxResumeParser pdfBoxResumeParser;
    private final OcrResumeParser ocrResumeParser;
    private final ResumeParsingDetector resumeParsingDetector;

    public ResumeParsingServiceImpl(PdfBoxResumeParser pdfBoxResumeParser,
                                    OcrResumeParser ocrResumeParser,
                                    ResumeParsingDetector resumeParsingDetector) {
        this.pdfBoxResumeParser = pdfBoxResumeParser;
        this.ocrResumeParser = ocrResumeParser;
        this.resumeParsingDetector = resumeParsingDetector;
    }

    @Override
    public ResumeParseResult parse(Path filePath) throws IOException {
        ResumeParseResult pdfResult = pdfBoxResumeParser.parse(filePath);
        ResumeParsingDecision decision = resumeParsingDetector.detect(pdfResult);

        if (!decision.isRequireOcrFallback()) {
            pdfResult.setParserLog(appendLog(pdfResult.getParserLog(), decision.getReason()));
            return pdfResult;
        }

        ResumeParseResult ocrResult = ocrResumeParser.parse(filePath);
        ocrResult.setParserLog(appendLog(ocrResult.getParserLog(), decision.getReason()));
        return ocrResult;
    }

    private String appendLog(String original, String suffix) {
        if (original == null || original.isBlank()) {
            return suffix;
        }
        return original + "；" + suffix;
    }
}
