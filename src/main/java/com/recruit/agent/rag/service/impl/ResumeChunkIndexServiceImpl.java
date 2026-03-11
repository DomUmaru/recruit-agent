package com.recruit.agent.rag.service.impl;

import com.recruit.agent.rag.chunk.ResumeChunkDraft;
import com.recruit.agent.rag.chunk.ResumeChunkingResult;
import com.recruit.agent.rag.model.ResumeChunk;
import com.recruit.agent.rag.repository.ResumeChunkRepository;
import com.recruit.agent.rag.service.ResumeChunkIndexService;
import com.recruit.agent.resume.model.ResumeDocument;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 简历分块索引服务实现。
 */
@Service
public class ResumeChunkIndexServiceImpl implements ResumeChunkIndexService {

    private final ResumeChunkRepository resumeChunkRepository;

    public ResumeChunkIndexServiceImpl(ResumeChunkRepository resumeChunkRepository) {
        this.resumeChunkRepository = resumeChunkRepository;
    }

    @Override
    public void indexChunks(ResumeDocument document, ResumeChunkingResult chunkingResult) {
        List<ResumeChunk> chunks = new ArrayList<>();
        for (ResumeChunkDraft draft : chunkingResult.getChunks()) {
            ResumeChunk chunk = new ResumeChunk();
            chunk.setId(UUID.randomUUID().toString());
            chunk.setCandidateId(document.getCandidate().getId());
            chunk.setDocId(document.getId());
            chunk.setParentId(draft.getParentId());
            chunk.setChunkType(draft.getChunkType().name());
            chunk.setSection(draft.getSection());
            chunk.setPage(draft.getPage());
            chunk.setChunkOrder(draft.getChunkOrder());
            chunk.setContent(draft.getContent());
            chunk.setNormalizedContent(draft.getContent());
            chunk.setTags(draft.getTags());
            chunk.setMetadata(draft.getMetadata());
            chunk.setIndexedAt(LocalDateTime.now());
            chunks.add(chunk);
        }
        resumeChunkRepository.saveAll(chunks);
    }
}
