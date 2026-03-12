package com.recruit.agent.interview.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import com.recruit.agent.interview.dto.InterviewQuestionRequest;
import com.recruit.agent.interview.service.impl.InterviewQuestionServiceImpl;
import com.recruit.agent.interview.vo.InterviewQuestionResponse;
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
    void shouldGenerateInterviewQuestions() {
        CandidateProfileIndexRepository profileRepository = org.mockito.Mockito.mock(CandidateProfileIndexRepository.class);
        ResumeChunkRepository chunkRepository = org.mockito.Mockito.mock(ResumeChunkRepository.class);
        InterviewQuestionServiceImpl service = new InterviewQuestionServiceImpl(profileRepository, chunkRepository);

        CandidateProfileIndex candidate = new CandidateProfileIndex();
        candidate.setCandidateId("candidate-1");
        candidate.setCandidateNo("C-001");
        candidate.setFullName("张三");
        candidate.setTotalYearsOfExperience(new BigDecimal("5.0"));
        candidate.setTechnicalSkills(List.of("Java", "Elasticsearch"));
        candidate.setOutsourcing(true);

        ResumeChunk chunk = new ResumeChunk();
        chunk.setSection("project");
        chunk.setPage(1);
        chunk.setChunkOrder(1);
        chunk.setContent("负责推荐系统召回优化");

        when(profileRepository.findByCandidateId("candidate-1")).thenReturn(Optional.of(candidate));
        when(chunkRepository.findByCandidateId("candidate-1")).thenReturn(List.of(chunk));

        InterviewQuestionRequest request = new InterviewQuestionRequest();
        request.setCandidateIds(List.of("candidate-1"));
        request.setTargetQuery("推荐系统");

        InterviewQuestionResponse response = service.generate(request);

        assertEquals(request.getTargetQuery(), response.getTargetQuery());
        assertEquals(1, response.getCandidates().size());
        assertFalse(response.getCandidates().get(0).getQuestions().isEmpty());
        assertFalse(response.getSummary().isBlank());
    }
}
