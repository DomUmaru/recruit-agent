package com.recruit.agent.rag.chunk;

import java.util.List;
import java.util.Map;

/**
 * 简历分块草稿对象，用于 chunking 过程中的中间产物。
 */
public class ResumeChunkDraft {

    /**
     * 父分块 ID。
     */
    private String parentId;

    /**
     * 分块类型。
     */
    private ChunkType chunkType;

    /**
     * 简历分段名称。
     */
    private String section;

    /**
     * 页码。
     */
    private Integer page;

    /**
     * 分块顺序。
     */
    private Integer chunkOrder;

    /**
     * 分块内容。
     */
    private String content;

    /**
     * 分块标签。
     */
    private List<String> tags;

    /**
     * 扩展元数据。
     */
    private Map<String, Object> metadata;

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public ChunkType getChunkType() {
        return chunkType;
    }

    public void setChunkType(ChunkType chunkType) {
        this.chunkType = chunkType;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getChunkOrder() {
        return chunkOrder;
    }

    public void setChunkOrder(Integer chunkOrder) {
        this.chunkOrder = chunkOrder;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
