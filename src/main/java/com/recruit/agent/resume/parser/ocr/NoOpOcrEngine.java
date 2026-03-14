package com.recruit.agent.resume.parser.ocr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adapter kept for the existing parser chain. Real OCR providers should implement {@link OcrService}.
 */
@Component
public class NoOpOcrEngine implements OcrEngine {

    private final OcrService ocrService;

    public NoOpOcrEngine(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @Override
    public boolean isAvailable() {
        return ocrService.isAvailable();
    }

    @Override
    public List<String> recognize(Path filePath) throws IOException {
        return ocrService.recognize(filePath).getPageTexts();
    }
}
