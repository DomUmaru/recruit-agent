package com.recruit.agent.rag.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruit.agent.candidate.model.Candidate;
import com.recruit.agent.rag.chunk.ChunkType;
import com.recruit.agent.rag.chunk.ResumeChunkDraft;
import com.recruit.agent.rag.chunk.ResumeChunkingResult;
import com.recruit.agent.rag.embedding.EmbeddingService;
import com.recruit.agent.rag.model.ResumeChunk;
import com.recruit.agent.rag.repository.ResumeChunkRepository;
import com.recruit.agent.rag.service.impl.ResumeChunkIndexServiceImpl;
import com.recruit.agent.resume.model.ResumeDocument;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ResumeChunkIndexServiceImplTest {

    @Test
    void shouldAssignEmbeddingsWhenProviderIsAvailable() throws IOException {
        ResumeChunkRepository repository = org.mockito.Mockito.mock(ResumeChunkRepository.class);
        EmbeddingService embeddingService = org.mockito.Mockito.mock(EmbeddingService.class);
        ResumeChunkIndexServiceImpl service = new ResumeChunkIndexServiceImpl(repository, embeddingService);

        when(embeddingService.isAvailable()).thenReturn(true);
        when(embeddingService.embedAll(anyList())).thenReturn(List.of(new float[]{0.1f, 0.2f}));

        service.indexChunks(document(), chunkingResult("Java Spring"));

        ArgumentCaptor<List<ResumeChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<ResumeChunk> chunks = captor.getValue();
        assertArrayEquals(new float[]{0.1f, 0.2f}, chunks.get(0).getEmbedding());
        verify(embeddingService).embedAll(List.of("Java Spring"));
    }

    @Test
    void shouldSkipEmbeddingsWhenProviderIsUnavailable() throws IOException {
        ResumeChunkRepository repository = org.mockito.Mockito.mock(ResumeChunkRepository.class);
        EmbeddingService embeddingService = org.mockito.Mockito.mock(EmbeddingService.class);
        ResumeChunkIndexServiceImpl service = new ResumeChunkIndexServiceImpl(repository, embeddingService);

        when(embeddingService.isAvailable()).thenReturn(false);

        service.indexChunks(document(), chunkingResult("Java Spring"));

        ArgumentCaptor<List<ResumeChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<ResumeChunk> chunks = captor.getValue();
        assertNull(chunks.get(0).getEmbedding());
        verify(embeddingService, never()).embedAll(anyList());
    }

    private ResumeDocument document() {
        Candidate candidate = new Candidate();
        candidate.setId("candidate-1");

        ResumeDocument document = new ResumeDocument();
        document.setId("doc-1");
        document.setCandidate(candidate);
        return document;
    }

    private ResumeChunkingResult chunkingResult(String content) {
        ResumeChunkDraft draft = new ResumeChunkDraft();
        draft.setParentId("parent-1");
        draft.setChunkType(ChunkType.CHILD);
        draft.setSection("experience");
        draft.setPage(1);
        draft.setChunkOrder(1);
        draft.setContent(content);

        ResumeChunkingResult result = new ResumeChunkingResult();
        result.setChunks(List.of(draft));
        return result;
    }
}
