package com.recruit.agent.rag.chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.candidate.model.Candidate;
import com.recruit.agent.resume.model.ResumeDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleBasedResumeSplitterTest {

    private final RuleBasedResumeSplitter splitter = new RuleBasedResumeSplitter(new ObjectMapper());

    @Test
    void shouldSplitResumeIntoStructuredParentAndChildChunks() throws Exception {
        ResumeDocument document = new ResumeDocument();
        Candidate candidate = new Candidate();
        candidate.setId("candidate-1");
        document.setId("doc-1");
        document.setVersionNo(1);
        document.setCandidate(candidate);
        document.setPageTextJson(new ObjectMapper().writeValueAsString(List.of("""
            教育经历
            重庆科技大学
            软件工程 本科

            项目经历
            高并发微服务秒杀系统
            负责秒杀链路设计与库存扣减优化
            基于 Redis 原子指令实现预扣减与削峰
            """)));

        ResumeChunkingResult result = splitter.chunk(document);

        assertEquals(5, result.getChunks().size());

        ResumeChunkDraft parentEducation = result.getChunks().get(0);
        ResumeChunkDraft childEducation = result.getChunks().get(1);
        ResumeChunkDraft parentProject = result.getChunks().get(2);
        ResumeChunkDraft childProject = result.getChunks().get(3);
        ResumeChunkDraft childProject2 = result.getChunks().get(4);

        assertEquals(ChunkType.PARENT, parentEducation.getChunkType());
        assertEquals("教育经历", parentEducation.getSection());
        assertEquals(ChunkType.CHILD, childEducation.getChunkType());
        assertEquals("教育经历", childEducation.getSection());
        assertEquals("[板块:教育经历][子项:未命名子项] 内容:重庆科技大学 软件工程 本科", childEducation.getNormalizedContent());

        assertEquals(ChunkType.PARENT, parentProject.getChunkType());
        assertEquals("项目经历", parentProject.getSection());
        assertEquals("高并发微服务秒杀系统", parentProject.getSubSectionTitle());
        assertTrue(parentProject.getContent().startsWith("高并发微服务秒杀系统"));

        assertEquals(ChunkType.CHILD, childProject.getChunkType());
        assertEquals(parentProject.getParentId(), childProject.getParentId());
        assertEquals("项目经历", childProject.getSection());
        assertEquals("高并发微服务秒杀系统", childProject.getSubSectionTitle());
        assertTrue(childProject.getNormalizedContent().contains("[板块:项目经历][子项:高并发微服务秒杀系统]"));
        assertEquals(parentProject.getParentId(), childProject2.getParentId());
        assertTrue(childProject2.getContent().startsWith("基于 Redis"));
        assertNotNull(childProject.getMetadata().get("section"));
    }

    @Test
    void shouldRecognizeSectionAliasesAndCleanOcrNoise() throws Exception {
        ResumeDocument document = new ResumeDocument();
        Candidate candidate = new Candidate();
        candidate.setId("candidate-2");
        document.setId("doc-2");
        document.setVersionNo(1);
        document.setCandidate(candidate);
        document.setPageTextJson(new ObjectMapper().writeValueAsString(List.of("""
            • 教育背景：
            重庆科技大学
            计算机科学与技术 本科

            Â· 专业技能
            Java、Spring Boot、Redis
            """)));

        ResumeChunkingResult result = splitter.chunk(document);

        assertEquals("教育经历", result.getChunks().get(0).getSection());
        assertEquals("专业技能", result.getChunks().get(2).getSection());
        assertTrue(result.getChunks().get(3).getNormalizedContent().startsWith("[板块:专业技能]"));
    }

    @Test
    void shouldSplitInlineSectionTitlesFromOcrMergedLines() throws Exception {
        ResumeDocument document = new ResumeDocument();
        Candidate candidate = new Candidate();
        candidate.setId("candidate-3");
        document.setId("doc-3");
        document.setVersionNo(1);
        document.setCandidate(candidate);
        document.setPageTextJson(new ObjectMapper().writeValueAsString(List.of("""
            github.com/test 教育背景
            重庆科技大学 计算机科学与技术
            2026.06 核心课程：数据结构 技术竞赛与奖项
            蓝桥杯省奖
            """)));

        ResumeChunkingResult result = splitter.chunk(document);

        assertEquals("通用信息", result.getChunks().get(0).getSection());
        assertEquals("教育经历", result.getChunks().get(2).getSection());
        assertEquals("获奖经历", result.getChunks().get(5).getSection());
    }

    @Test
    void shouldNotUseTimeLineAsInitialSubSectionTitle() throws Exception {
        ResumeDocument document = new ResumeDocument();
        Candidate candidate = new Candidate();
        candidate.setId("candidate-4");
        document.setId("doc-4");
        document.setVersionNo(1);
        document.setCandidate(candidate);
        document.setPageTextJson(new ObjectMapper().writeValueAsString(List.of("""
            项目经历
            2025.11
            高性能 KV 存储引擎
            基于跳表实现内存索引
            """)));

        ResumeChunkingResult result = splitter.chunk(document);

        ResumeChunkDraft parentProject = result.getChunks().get(0);
        ResumeChunkDraft childFirst = result.getChunks().get(1);

        assertEquals("项目经历", parentProject.getSection());
        assertEquals(null, parentProject.getSubSectionTitle());
        assertEquals("2025.11 高性能 KV 存储引擎", childFirst.getContent());
    }
}
