package com.recruit.agent.resume.service;

import com.recruit.agent.resume.model.ResumeDocument;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 简历摄入服务接口。
 */
public interface ResumeIngestionService {

    /**
     * 执行简历文档的基础摄入处理。
     *
     * @param document 简历文档
     * @param filePath 文件路径
     * @throws IOException 文件处理异常
     */
    void ingest(ResumeDocument document, Path filePath) throws IOException;
}
