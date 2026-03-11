package com.recruit.agent.rag.chunk;

import com.recruit.agent.resume.model.ResumeDocument;

/**
 * 简历分块服务接口。
 */
public interface ResumeChunkingService {

    /**
     * 对简历文档执行 Parent-Child Chunking。
     *
     * @param document 简历文档
     * @return chunking 结果
     */
    ResumeChunkingResult chunk(ResumeDocument document);
}
