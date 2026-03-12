package com.recruit.agent.resume.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.recruit.agent.resume.model.ResumeParseType;
import com.recruit.agent.resume.parser.ocr.OcrEngine;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class OcrResumeParserTest {

    @Test
    void shouldReturnPlaceholderResultWhenOcrEngineIsUnavailable() throws IOException {
        OcrEngine ocrEngine = new OcrEngine() {
            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public List<String> recognize(Path filePath) {
                return List.of();
            }
        };

        OcrResumeParser parser = new OcrResumeParser(ocrEngine, new DefaultResumeTextCleaner());

        ResumeParseResult result = parser.parse(Path.of("resume.pdf"));

        assertEquals(ResumeParseType.OCR_SCANNED, result.getParseType());
        assertFalse(result.isTextExtractable());
        assertEquals(0, result.getPageCount());
        assertTrue(result.getParserLog().contains("OCR"));
    }

    @Test
    void shouldBuildParseResultFromOcrPages() throws IOException {
        OcrEngine ocrEngine = new OcrEngine() {
            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public List<String> recognize(Path filePath) {
                return List.of("第一页 Java", "第二页 Elasticsearch");
            }
        };

        OcrResumeParser parser = new OcrResumeParser(ocrEngine, new DefaultResumeTextCleaner());

        ResumeParseResult result = parser.parse(Path.of("resume.pdf"));

        assertEquals(ResumeParseType.OCR_SCANNED, result.getParseType());
        assertTrue(result.isTextExtractable());
        assertEquals(2, result.getPageCount());
        assertEquals("第一页 Java\n\n第二页 Elasticsearch", result.getRawText());
        assertEquals(List.of("第一页 Java", "第二页 Elasticsearch"), result.getPageTexts());
        assertTrue(result.getParserLog().contains("OCR"));
    }
}
