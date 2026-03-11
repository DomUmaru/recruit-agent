package com.recruit.agent.rag.chunk;

import java.util.List;

/**
 * 简历 chunking 结果对象。
 */
public class ResumeChunkingResult {

    /**
     * 分块结果列表。
     */
    private List<ResumeChunkDraft> chunks;

    /**
     * 处理说明。
     */
    private String log;

    public List<ResumeChunkDraft> getChunks() {
        return chunks;
    }

    public void setChunks(List<ResumeChunkDraft> chunks) {
        this.chunks = chunks;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }
}
