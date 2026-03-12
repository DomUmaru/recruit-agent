package com.recruit.agent.rag.service.impl;

import com.recruit.agent.rag.embedding.EmbeddingService;
import com.recruit.agent.rag.chunk.ResumeChunkDraft;
import com.recruit.agent.rag.chunk.ResumeChunkingResult;
import com.recruit.agent.rag.model.ResumeChunk;
import com.recruit.agent.rag.repository.ResumeChunkRepository;
import com.recruit.agent.rag.service.ResumeChunkIndexService;
import com.recruit.agent.resume.model.ResumeDocument;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 简历分块索引服务实现。
 */
@Service
public class ResumeChunkIndexServiceImpl implements ResumeChunkIndexService {

    private static final Logger log = LoggerFactory.getLogger(ResumeChunkIndexServiceImpl.class);

    private final ResumeChunkRepository resumeChunkRepository;
    private final EmbeddingService embeddingService;

    public ResumeChunkIndexServiceImpl(ResumeChunkRepository resumeChunkRepository,
                                       EmbeddingService embeddingService) {
        this.resumeChunkRepository = resumeChunkRepository;
        this.embeddingService = embeddingService;
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
        enrichEmbeddings(chunks);
        resumeChunkRepository.saveAll(chunks);
    }

    private void enrichEmbeddings(List<ResumeChunk> chunks) {
        if (chunks.isEmpty() || !embeddingService.isAvailable()) {
            return;
        }

        List<String> texts = chunks.stream()
            .map(this::resolveEmbeddingText)
            .toList();

        try {
            List<float[]> embeddings = embeddingService.embedAll(texts);
            if (embeddings.size() != chunks.size()) {
                log.warn("Embedding result size mismatch. expected={}, actual={}", chunks.size(), embeddings.size());
                return;
            }
            for (int index = 0; index < chunks.size(); index++) {
                float[] vector = embeddings.get(index);
                if (vector != null && vector.length > 0) {
                    chunks.get(index).setEmbedding(vector);
                }
            }
        } catch (IOException exception) {
            log.warn("Failed to generate resume chunk embeddings. Fallback to keyword-only indexing.", exception);
        }
    }

    private String resolveEmbeddingText(ResumeChunk chunk) {
        if (chunk.getNormalizedContent() != null && !chunk.getNormalizedContent().isBlank()) {
            return chunk.getNormalizedContent();
        }
        return chunk.getContent() == null ? "" : chunk.getContent();
    }
}
