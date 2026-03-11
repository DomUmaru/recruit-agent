package com.recruit.agent.resume.parser;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 简历解析编排服务接口。
 */
public interface ResumeParsingService {

    /**
     * 执行简历解析，并在需要时走 OCR 降级。
     *
     * @param filePath 文件路径
     * @return 解析结果
     * @throws IOException 文件处理异常
     */
    ResumeParseResult parse(Path filePath) throws IOException;
}
