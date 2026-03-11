package com.recruit.agent.rag.service;

import com.recruit.agent.rag.chunk.ResumeChunkingResult;
import com.recruit.agent.resume.model.ResumeDocument;

/**
 * 简历分块索引服务接口。
 */
public interface ResumeChunkIndexService {

    /**
     * 将 chunking 结果写入检索索引。
     *
     * @param document 简历文档
     * @param chunkingResult 分块结果
     */
    void indexChunks(ResumeDocument document, ResumeChunkingResult chunkingResult);
}
