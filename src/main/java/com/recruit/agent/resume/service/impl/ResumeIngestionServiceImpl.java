package com.recruit.agent.resume.service.impl;

import com.recruit.agent.resume.model.ResumeDocument;
import com.recruit.agent.resume.model.ResumeDocumentStatus;
import com.recruit.agent.resume.parser.ResumeParseResult;
import com.recruit.agent.resume.parser.ResumeParsingService;
import com.recruit.agent.resume.repository.ResumeDocumentRepository;
import com.recruit.agent.resume.service.ResumeIngestionService;
import com.recruit.agent.candidate.profile.CandidateProfileIndexService;
import com.recruit.agent.rag.chunk.ResumeChunkingResult;
import com.recruit.agent.rag.chunk.ResumeChunkingService;
import com.recruit.agent.rag.service.ResumeChunkIndexService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 简历摄入服务实现，当前先完成状态流转与解析器占位接入。
 */
@Service
public class ResumeIngestionServiceImpl implements ResumeIngestionService {

    private final ResumeDocumentRepository resumeDocumentRepository;
    private final ResumeParsingService resumeParsingService;
    private final ResumeChunkingService resumeChunkingService;
    private final ResumeChunkIndexService resumeChunkIndexService;
    private final CandidateProfileIndexService candidateProfileIndexService;
    private final ObjectMapper objectMapper;

    public ResumeIngestionServiceImpl(ResumeDocumentRepository resumeDocumentRepository,
                                      ResumeParsingService resumeParsingService,
                                      ResumeChunkingService resumeChunkingService,
                                      ResumeChunkIndexService resumeChunkIndexService,
                                      CandidateProfileIndexService candidateProfileIndexService,
                                      ObjectMapper objectMapper) {
        this.resumeDocumentRepository = resumeDocumentRepository;
        this.resumeParsingService = resumeParsingService;
        this.resumeChunkingService = resumeChunkingService;
        this.resumeChunkIndexService = resumeChunkIndexService;
        this.candidateProfileIndexService = candidateProfileIndexService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ingest(ResumeDocument document, Path filePath) throws IOException {
        document.setStatus(ResumeDocumentStatus.PARSING);
        resumeDocumentRepository.save(document);

        ResumeParseResult parseResult = resumeParsingService.parse(filePath);

        document.setParseType(parseResult.getParseType());
        document.setRawText(parseResult.getRawText());
        document.setCleanedText(parseResult.getCleanedText());
        document.setPageTextJson(writeAsJson(parseResult.getPageTexts()));
        document.setPageCount(parseResult.getPageCount());
        document.setParserLog(parseResult.getParserLog());
        document.setStatus(ResumeDocumentStatus.PARSED);

        if (!parseResult.isTextExtractable()) {
            document.setStatus(ResumeDocumentStatus.FAILED);
            document.setParserLog(appendLog(parseResult.getParserLog(), "当前版本暂未接入 OCR 自动降级"));
        }

        resumeDocumentRepository.save(document);

        if (ResumeDocumentStatus.PARSED.equals(document.getStatus())) {
            ResumeChunkingResult chunkingResult = resumeChunkingService.chunk(document);
            document.setParserLog(appendLog(document.getParserLog(), chunkingResult.getLog()));
            resumeDocumentRepository.save(document);
            resumeChunkIndexService.indexChunks(document, chunkingResult);
            candidateProfileIndexService.indexProfile(document);
            document.setStatus(ResumeDocumentStatus.INDEXED);
            resumeDocumentRepository.save(document);
        }
    }

    private String writeAsJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化解析结果失败", ex);
        }
    }

    private String appendLog(String original, String suffix) {
        if (original == null || original.isBlank()) {
            return suffix;
        }
        return original + "；" + suffix;
    }
}
