package com.recruit.agent.resume.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.candidate.model.Candidate;
import com.recruit.agent.candidate.profile.CandidateProfileIndexService;
import com.recruit.agent.rag.chunk.ChunkType;
import com.recruit.agent.rag.chunk.ResumeChunkDraft;
import com.recruit.agent.rag.chunk.ResumeChunkingResult;
import com.recruit.agent.rag.chunk.ResumeChunkingService;
import com.recruit.agent.rag.service.ResumeChunkIndexService;
import com.recruit.agent.resume.model.ResumeDocument;
import com.recruit.agent.resume.model.ResumeDocumentStatus;
import com.recruit.agent.resume.model.ResumeParseType;
import com.recruit.agent.resume.parser.ResumeParseResult;
import com.recruit.agent.resume.parser.ResumeParsingService;
import com.recruit.agent.resume.repository.ResumeDocumentRepository;
import com.recruit.agent.resume.service.impl.ResumeIngestionServiceImpl;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResumeIngestionServiceImplTest {

    @Test
    void shouldParseChunkAndIndexWhenResumeIsTextPdf() throws IOException {
        ResumeDocumentRepository repository = org.mockito.Mockito.mock(ResumeDocumentRepository.class);
        ResumeParsingService parsingService = org.mockito.Mockito.mock(ResumeParsingService.class);
        ResumeChunkingService chunkingService = org.mockito.Mockito.mock(ResumeChunkingService.class);
        ResumeChunkIndexService chunkIndexService = org.mockito.Mockito.mock(ResumeChunkIndexService.class);
        CandidateProfileIndexService candidateProfileIndexService = org.mockito.Mockito.mock(CandidateProfileIndexService.class);

        ResumeIngestionServiceImpl service = new ResumeIngestionServiceImpl(
            repository,
            parsingService,
            chunkingService,
            chunkIndexService,
            candidateProfileIndexService,
            new ObjectMapper()
        );

        Candidate candidate = new Candidate();
        candidate.setId("candidate-1");
        candidate.setCandidateNo("C-001");
        candidate.setFullName("张三");
        candidate.setTotalYearsOfExperience(BigDecimal.valueOf(5));

        ResumeDocument document = new ResumeDocument();
        document.setId("doc-1");
        document.setCandidate(candidate);
        document.setVersionNo(1);

        ResumeParseResult parseResult = new ResumeParseResult();
        parseResult.setParseType(ResumeParseType.PDF_TEXT);
        parseResult.setRawText("Java Spring");
        parseResult.setCleanedText("Java Spring");
        parseResult.setPageTexts(List.of("Java Spring"));
        parseResult.setPageCount(1);
        parseResult.setTextExtractable(true);
        parseResult.setParserLog("PDFBox 文本提取成功");

        ResumeChunkDraft chunkDraft = new ResumeChunkDraft();
        chunkDraft.setParentId("parent-1");
        chunkDraft.setChunkType(ChunkType.PARENT);
        chunkDraft.setSection("skills");
        chunkDraft.setPage(1);
        chunkDraft.setChunkOrder(0);
        chunkDraft.setContent("Java Spring");
        chunkDraft.setTags(List.of("skills"));
        chunkDraft.setMetadata(Map.of("candidateId", "candidate-1"));

        ResumeChunkingResult chunkingResult = new ResumeChunkingResult();
        chunkingResult.setChunks(List.of(chunkDraft));
        chunkingResult.setLog("已完成 Parent-Child Chunking，生成 1 个分块");

        when(parsingService.parse(any(Path.class))).thenReturn(parseResult);
        when(chunkingService.chunk(document)).thenReturn(chunkingResult);
        when(repository.save(any(ResumeDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.ingest(document, Path.of("test.pdf"));

        assertEquals(ResumeDocumentStatus.INDEXED, document.getStatus());
        assertEquals(ResumeParseType.PDF_TEXT, document.getParseType());
        assertEquals(1, document.getPageCount());

        verify(chunkIndexService).indexChunks(document, chunkingResult);
        verify(candidateProfileIndexService).indexProfile(document);
    }

    @Test
    void shouldMarkFailedWhenTextNotExtractable() throws IOException {
        ResumeDocumentRepository repository = org.mockito.Mockito.mock(ResumeDocumentRepository.class);
        ResumeParsingService parsingService = org.mockito.Mockito.mock(ResumeParsingService.class);
        ResumeChunkingService chunkingService = org.mockito.Mockito.mock(ResumeChunkingService.class);
        ResumeChunkIndexService chunkIndexService = org.mockito.Mockito.mock(ResumeChunkIndexService.class);
        CandidateProfileIndexService candidateProfileIndexService = org.mockito.Mockito.mock(CandidateProfileIndexService.class);

        ResumeIngestionServiceImpl service = new ResumeIngestionServiceImpl(
            repository,
            parsingService,
            chunkingService,
            chunkIndexService,
            candidateProfileIndexService,
            new ObjectMapper()
        );

        ResumeDocument document = new ResumeDocument();
        document.setId("doc-2");

        ResumeParseResult parseResult = new ResumeParseResult();
        parseResult.setParseType(ResumeParseType.OCR_SCANNED);
        parseResult.setRawText("");
        parseResult.setCleanedText("");
        parseResult.setPageTexts(List.of());
        parseResult.setPageCount(0);
        parseResult.setTextExtractable(false);
        parseResult.setParserLog("OCR 引擎不可用");

        when(parsingService.parse(any(Path.class))).thenReturn(parseResult);
        when(repository.save(any(ResumeDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.ingest(document, Path.of("scan.pdf"));

        assertEquals(ResumeDocumentStatus.FAILED, document.getStatus());
        verify(chunkingService, never()).chunk(any());
        verify(chunkIndexService, never()).indexChunks(any(), any());
        verify(candidateProfileIndexService, never()).indexProfile(any());
    }
}
