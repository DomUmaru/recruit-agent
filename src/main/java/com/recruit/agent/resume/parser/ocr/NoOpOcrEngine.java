package com.recruit.agent.resume.parser.ocr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 默认 OCR 引擎占位实现。
 */
@Component
public class NoOpOcrEngine implements OcrEngine {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public List<String> recognize(Path filePath) throws IOException {
        return List.of();
    }
}
