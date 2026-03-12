package com.recruit.agent.resume.parser.ocr;

import java.io.IOException;
import java.nio.file.Path;

public interface OcrService {

    boolean isAvailable();

    OcrResult recognize(Path filePath) throws IOException;
}
