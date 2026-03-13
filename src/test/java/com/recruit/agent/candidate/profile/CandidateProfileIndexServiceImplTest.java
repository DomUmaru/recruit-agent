package com.recruit.agent.candidate.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruit.agent.candidate.model.Candidate;
import com.recruit.agent.candidate.model.CandidateSource;
import com.recruit.agent.candidate.model.CandidateStatus;
import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import com.recruit.agent.candidate.repository.CandidateRepository;
import com.recruit.agent.rag.embedding.EmbeddingService;
import com.recruit.agent.rag.model.CandidateProfileIndex;
import com.recruit.agent.rag.repository.CandidateProfileIndexRepository;
import com.recruit.agent.resume.model.ResumeDocument;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CandidateProfileIndexServiceImplTest {

    @Test
    void shouldWriteProfileEmbeddingWhenProviderAvailable() throws Exception {
        CandidateProfileExtractor extractor = org.mockito.Mockito.mock(CandidateProfileExtractor.class);
        CandidateRepository candidateRepository = org.mockito.Mockito.mock(CandidateRepository.class);
        CandidateProfileIndexRepository indexRepository = org.mockito.Mockito.mock(CandidateProfileIndexRepository.class);
        EmbeddingService embeddingService = org.mockito.Mockito.mock(EmbeddingService.class);

        when(indexRepository.findByCandidateId("candidate-1")).thenReturn(Optional.empty());
        when(embeddingService.isAvailable()).thenReturn(true);
        when(embeddingService.embedAll(any())).thenReturn(List.of(new float[]{0.1f, 0.2f}));

        CandidateProfileIndexServiceImpl service = new CandidateProfileIndexServiceImpl(
            extractor, candidateRepository, indexRepository, embeddingService
        );

        ResumeDocument document = new ResumeDocument();
        document.setCandidate(buildCandidate());
        when(extractor.extract(document)).thenReturn(buildDraft());

        service.indexProfile(document);

        ArgumentCaptor<CandidateProfileIndex> captor = ArgumentCaptor.forClass(CandidateProfileIndex.class);
        verify(indexRepository).save(captor.capture());
        CandidateProfileIndex saved = captor.getValue();

        assertNotNull(saved.getEmbedding());
        assertEquals(2, saved.getEmbedding().length);
        verify(embeddingService).embedAll(any());
    }

    @Test
    void shouldFallbackWhenProfileEmbeddingFails() throws Exception {
        CandidateProfileExtractor extractor = org.mockito.Mockito.mock(CandidateProfileExtractor.class);
        CandidateRepository candidateRepository = org.mockito.Mockito.mock(CandidateRepository.class);
        CandidateProfileIndexRepository indexRepository = org.mockito.Mockito.mock(CandidateProfileIndexRepository.class);
        EmbeddingService embeddingService = org.mockito.Mockito.mock(EmbeddingService.class);

        when(indexRepository.findByCandidateId("candidate-1")).thenReturn(Optional.empty());
        when(embeddingService.isAvailable()).thenReturn(true);
        when(embeddingService.embedAll(any())).thenThrow(new IOException("embedding failed"));

        CandidateProfileIndexServiceImpl service = new CandidateProfileIndexServiceImpl(
            extractor, candidateRepository, indexRepository, embeddingService
        );

        ResumeDocument document = new ResumeDocument();
        document.setCandidate(buildCandidate());
        when(extractor.extract(document)).thenReturn(buildDraft());

        service.indexProfile(document);

        ArgumentCaptor<CandidateProfileIndex> captor = ArgumentCaptor.forClass(CandidateProfileIndex.class);
        verify(indexRepository).save(captor.capture());
        CandidateProfileIndex saved = captor.getValue();

        assertNull(saved.getEmbedding());
    }

    private Candidate buildCandidate() {
        Candidate candidate = new Candidate();
        candidate.setId("candidate-1");
        candidate.setCandidateNo("C-001");
        candidate.setFullName("张三");
        candidate.setCurrentCity("上海");
        candidate.setSource(CandidateSource.MANUAL_IMPORT);
        candidate.setStatus(CandidateStatus.ACTIVE);
        return candidate;
    }

    private CandidateProfileDraft buildDraft() {
        CandidateProfileDraft draft = new CandidateProfileDraft();
        draft.setHighestDegree(DegreeLevel.BACHELOR);
        draft.setSchoolName("同济大学");
        draft.setSchoolTier(SchoolTier.PROJECT_985);
        draft.setTotalYearsOfExperience(new BigDecimal("5.0"));
        draft.setTechnicalSkills(List.of("Java", "Elasticsearch"));
        draft.setIndustryTags(List.of("企业服务"));
        draft.setCompanyTags(List.of("腾讯"));
        draft.setProjectTags(List.of("搜索"));
        draft.setBigTech(true);
        draft.setOutsourcing(false);
        draft.setProfileSummary("负责搜索召回与排序优化");
        return draft;
    }
}
