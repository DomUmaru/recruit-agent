package com.recruit.agent.rag.chunk;

import java.util.List;
import java.util.Map;

public class ResumeChunkDraft {

    private String parentId;

    private ChunkType chunkType;

    private String section;

    private String subSectionTitle;

    private Integer page;

    private Integer chunkOrder;

    private String content;

    private String normalizedContent;

    private List<String> tags;

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

    public String getSubSectionTitle() {
        return subSectionTitle;
    }

    public void setSubSectionTitle(String subSectionTitle) {
        this.subSectionTitle = subSectionTitle;
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

    public String getNormalizedContent() {
        return normalizedContent;
    }

    public void setNormalizedContent(String normalizedContent) {
        this.normalizedContent = normalizedContent;
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
