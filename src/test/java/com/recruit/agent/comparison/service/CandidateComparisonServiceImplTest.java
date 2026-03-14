package com.recruit.agent.comparison.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.recruit.agent.comparison.dto.CandidateComparisonRequest;
import com.recruit.agent.comparison.service.impl.CandidateComparisonServiceImpl;
import com.recruit.agent.comparison.vo.CandidateComparisonResponse;
import com.recruit.agent.llm.LlmGenerationService;
import com.recruit.agent.rag.model.CandidateProfileIndex;
import com.recruit.agent.rag.model.ResumeChunk;
import com.recruit.agent.rag.repository.CandidateProfileIndexRepository;
import com.recruit.agent.rag.repository.ResumeChunkRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CandidateComparisonServiceImplTest {

    @Test
    void shouldBuildComparisonResponse() {
        CandidateProfileIndexRepository profileRepository = org.mockito.Mockito.mock(CandidateProfileIndexRepository.class);
        ResumeChunkRepository chunkRepository = org.mockito.Mockito.mock(ResumeChunkRepository.class);
        LlmGenerationService llmGenerationService = org.mockito.Mockito.mock(LlmGenerationService.class);
        CandidateComparisonServiceImpl service =
            new CandidateComparisonServiceImpl(profileRepository, chunkRepository, llmGenerationService);

        CandidateProfileIndex candidate1 = new CandidateProfileIndex();
        candidate1.setCandidateId("candidate-1");
        candidate1.setCandidateNo("C-001");
        candidate1.setFullName("张三");
        candidate1.setSchoolTier("PROJECT_985");
        candidate1.setTotalYearsOfExperience(new BigDecimal("5.0"));
        candidate1.setTechnicalSkills(List.of("Java", "Elasticsearch"));
        candidate1.setBigTech(true);
        candidate1.setOutsourcing(false);

        CandidateProfileIndex candidate2 = new CandidateProfileIndex();
        candidate2.setCandidateId("candidate-2");
        candidate2.setCandidateNo("C-002");
        candidate2.setFullName("李四");
        candidate2.setTotalYearsOfExperience(new BigDecimal("3.0"));
        candidate2.setTechnicalSkills(List.of("Java"));
        candidate2.setOutsourcing(true);

        ResumeChunk chunk = new ResumeChunk();
        chunk.setSection("project");
        chunk.setPage(1);
        chunk.setChunkOrder(1);
        chunk.setContent("负责搜索系统召回优化");

        when(profileRepository.findByCandidateId("candidate-1")).thenReturn(Optional.of(candidate1));
        when(profileRepository.findByCandidateId("candidate-2")).thenReturn(Optional.of(candidate2));
        when(chunkRepository.findByCandidateId("candidate-1")).thenReturn(List.of(chunk));
        when(chunkRepository.findByCandidateId("candidate-2")).thenReturn(List.of(chunk));
        when(llmGenerationService.isAvailable()).thenReturn(false);

        CandidateComparisonRequest request = new CandidateComparisonRequest();
        request.setCandidateIds(List.of("candidate-1", "candidate-2"));
        request.setTargetQuery("搜索系统");

        CandidateComparisonResponse response = service.compare(request);

        assertEquals(request.getTargetQuery(), response.getTargetQuery());
        assertEquals(2, response.getCandidates().size());
        assertEquals(1, response.getCandidates().get(0).getRank());
        assertEquals(2, response.getCandidates().get(1).getRank());
        assertFalse(response.getCandidates().get(0).getHighlights().isEmpty());
        assertFalse(response.getCandidates().get(1).getRiskPoints().isEmpty());
        assertFalse(response.getSummary().isBlank());
    }

    @Test
    void shouldUseLlmSummaryWhenAvailable() {
        CandidateProfileIndexRepository profileRepository = org.mockito.Mockito.mock(CandidateProfileIndexRepository.class);
        ResumeChunkRepository chunkRepository = org.mockito.Mockito.mock(ResumeChunkRepository.class);
        LlmGenerationService llmGenerationService = org.mockito.Mockito.mock(LlmGenerationService.class);
        CandidateComparisonServiceImpl service =
            new CandidateComparisonServiceImpl(profileRepository, chunkRepository, llmGenerationService);

        CandidateProfileIndex candidate = new CandidateProfileIndex();
        candidate.setCandidateId("candidate-1");
        candidate.setCandidateNo("C-001");
        candidate.setFullName("张三");
        candidate.setTotalYearsOfExperience(new BigDecimal("5.0"));
        candidate.setTechnicalSkills(List.of("Java", "Elasticsearch"));

        when(profileRepository.findByCandidateId("candidate-1")).thenReturn(Optional.of(candidate));
        when(chunkRepository.findByCandidateId("candidate-1")).thenReturn(List.of());
        when(llmGenerationService.isAvailable()).thenReturn(true);
        when(llmGenerationService.generate(anyString(), anyString())).thenReturn("这是 LLM 生成的对比总结。");

        CandidateComparisonRequest request = new CandidateComparisonRequest();
        request.setCandidateIds(List.of("candidate-1"));

        CandidateComparisonResponse response = service.compare(request);

        assertEquals("这是 LLM 生成的对比总结。", response.getSummary());
    }
}
