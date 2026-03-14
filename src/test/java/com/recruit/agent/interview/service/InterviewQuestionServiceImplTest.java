package com.recruit.agent.interview.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.recruit.agent.interview.dto.InterviewQuestionRequest;
import com.recruit.agent.interview.service.impl.InterviewQuestionServiceImpl;
import com.recruit.agent.interview.vo.InterviewQuestionResponse;
import com.recruit.agent.llm.LlmGenerationService;
import com.recruit.agent.position.repository.PositionJDRepository;
import com.recruit.agent.rag.model.CandidateProfileIndex;
import com.recruit.agent.rag.model.ResumeChunk;
import com.recruit.agent.rag.repository.CandidateProfileIndexRepository;
import com.recruit.agent.rag.repository.ResumeChunkRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InterviewQuestionServiceImplTest {

    @Test
    void shouldGenerateInterviewHandoffItemsUsingProjectEvidence() {
        CandidateProfileIndexRepository profileRepository = org.mockito.Mockito.mock(CandidateProfileIndexRepository.class);
        ResumeChunkRepository chunkRepository = org.mockito.Mockito.mock(ResumeChunkRepository.class);
        PositionJDRepository positionJDRepository = org.mockito.Mockito.mock(PositionJDRepository.class);
        LlmGenerationService llmGenerationService = org.mockito.Mockito.mock(LlmGenerationService.class);
        InterviewQuestionServiceImpl service =
            new InterviewQuestionServiceImpl(profileRepository, chunkRepository, positionJDRepository, llmGenerationService);

        CandidateProfileIndex candidate = new CandidateProfileIndex();
        candidate.setCandidateId("candidate-1");
        candidate.setCandidateNo("C-001");
        candidate.setFullName("张三");
        candidate.setTotalYearsOfExperience(new BigDecimal("5.0"));
        candidate.setTechnicalSkills(List.of("Java", "Elasticsearch"));
        candidate.setOutsourcing(true);

        ResumeChunk projectChunk = new ResumeChunk();
        projectChunk.setSection("项目经历");
        projectChunk.setPage(1);
        projectChunk.setChunkOrder(2);
        projectChunk.setContent("负责搜索系统召回优化，使用 Java 和 Elasticsearch。");

        ResumeChunk educationChunk = new ResumeChunk();
        educationChunk.setSection("教育经历");
        educationChunk.setPage(1);
        educationChunk.setChunkOrder(1);
        educationChunk.setContent("某大学 计算机硕士");

        when(profileRepository.findByCandidateId("candidate-1")).thenReturn(Optional.of(candidate));
        when(chunkRepository.findByCandidateId("candidate-1")).thenReturn(List.of(educationChunk, projectChunk));

        InterviewQuestionRequest request = new InterviewQuestionRequest();
        request.setCandidateIds(List.of("candidate-1"));
        request.setTargetQuery("搜索系统 Java 后端");

        InterviewQuestionResponse response = service.generate(request);

        assertEquals(request.getTargetQuery(), response.getTargetQuery());
        assertEquals(1, response.getCandidates().size());
        assertEquals(1, response.getCandidates().get(0).getRank());
        assertFalse(response.getCandidates().get(0).getQuestions().isEmpty());
        assertEquals("项目经历", response.getCandidates().get(0).getQuestions().get(0).getEvidenceList().get(0).getSection());
        assertTrue(response.getSummary().contains("【推荐理由】"));
        assertTrue(response.getSummary().contains("【风险点/存疑点】"));
        assertTrue(response.getSummary().contains("【追问建议】"));
    }

    @Test
    void shouldUseDeterministicSummaryEvenWhenLlmIsAvailable() {
        CandidateProfileIndexRepository profileRepository = org.mockito.Mockito.mock(CandidateProfileIndexRepository.class);
        ResumeChunkRepository chunkRepository = org.mockito.Mockito.mock(ResumeChunkRepository.class);
        PositionJDRepository positionJDRepository = org.mockito.Mockito.mock(PositionJDRepository.class);
        LlmGenerationService llmGenerationService = org.mockito.Mockito.mock(LlmGenerationService.class);
        InterviewQuestionServiceImpl service =
            new InterviewQuestionServiceImpl(profileRepository, chunkRepository, positionJDRepository, llmGenerationService);

        CandidateProfileIndex candidate = new CandidateProfileIndex();
        candidate.setCandidateId("candidate-1");
        candidate.setCandidateNo("C-001");
        candidate.setFullName("张三");
        candidate.setTotalYearsOfExperience(new BigDecimal("5.0"));
        candidate.setTechnicalSkills(List.of("Java", "Elasticsearch"));

        when(profileRepository.findByCandidateId("candidate-1")).thenReturn(Optional.of(candidate));
        when(chunkRepository.findByCandidateId("candidate-1")).thenReturn(List.of());

        InterviewQuestionRequest request = new InterviewQuestionRequest();
        request.setCandidateIds(List.of("candidate-1"));

        InterviewQuestionResponse response = service.generate(request);

        assertTrue(response.getSummary().contains("【推荐理由】"));
        assertTrue(response.getSummary().contains("需要进一步验证"));
    }
}
