package com.recruit.agent.resume.parser.ocr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * OCR 引擎接口。
 */
public interface OcrEngine {

    /**
     * 是否可用。
     *
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 执行 OCR 文本识别。
     *
     * @param filePath 文件路径
     * @return 按页文本列表
     * @throws IOException 文件处理异常
     */
    List<String> recognize(Path filePath) throws IOException;
}
