package com.recruit.agent.resume.parser.ocr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoOpOcrEngineTest {

    @Test
    void shouldDelegateAvailabilityToOcrService() {
        OcrService ocrService = new OcrService() {
            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public OcrResult recognize(Path filePath) {
                OcrResult result = new OcrResult();
                result.setPageTexts(List.of("page-1"));
                result.setRawText("page-1");
                result.setEngineName("stub");
                return result;
            }
        };

        NoOpOcrEngine engine = new NoOpOcrEngine(ocrService);

        assertTrue(engine.isAvailable());
    }

    @Test
    void shouldDelegateRecognitionToOcrService() throws IOException {
        OcrService ocrService = new OcrService() {
            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public OcrResult recognize(Path filePath) {
                OcrResult result = new OcrResult();
                result.setPageTexts(List.of("page-1", "page-2"));
                result.setRawText("page-1\n\npage-2");
                result.setEngineName("stub");
                return result;
            }
        };

        NoOpOcrEngine engine = new NoOpOcrEngine(ocrService);

        assertEquals(List.of("page-1", "page-2"), engine.recognize(Path.of("resume.pdf")));
        assertFalse(engine.isAvailable());
    }
}
