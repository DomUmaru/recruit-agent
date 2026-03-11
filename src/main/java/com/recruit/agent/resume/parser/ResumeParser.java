package com.recruit.agent.resume.parser;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 简历解析器统一接口。
 */
public interface ResumeParser {

    /**
     * 判断当前解析器是否支持指定文件。
     *
     * @param filePath 文件路径
     * @return 是否支持
     */
    boolean supports(Path filePath);

    /**
     * 解析简历文件。
     *
     * @param filePath 文件路径
     * @return 解析结果
     * @throws IOException 文件读取异常
     */
    ResumeParseResult parse(Path filePath) throws IOException;
}
