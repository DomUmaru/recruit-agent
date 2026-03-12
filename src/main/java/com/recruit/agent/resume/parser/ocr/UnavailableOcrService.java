package com.recruit.agent.resume.parser.ocr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ocr", name = "provider", havingValue = "none", matchIfMissing = true)
public class UnavailableOcrService implements OcrService {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public OcrResult recognize(Path filePath) throws IOException {
        OcrResult result = new OcrResult();
        result.setPageTexts(List.of());
        result.setRawText("");
        result.setEngineName("none");
        return result;
    }
}
