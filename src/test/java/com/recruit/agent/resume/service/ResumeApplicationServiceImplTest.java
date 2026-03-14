package com.recruit.agent.resume.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruit.agent.candidate.model.Candidate;
import com.recruit.agent.candidate.model.CandidateSource;
import com.recruit.agent.candidate.model.CandidateStatus;
import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import com.recruit.agent.candidate.repository.CandidateRepository;
import com.recruit.agent.candidate.school.SchoolTierResolver;
import com.recruit.agent.resume.dto.ResumeCandidateInfo;
import com.recruit.agent.resume.dto.ResumeUploadRequest;
import com.recruit.agent.resume.dto.ResumeUploadResponse;
import com.recruit.agent.resume.model.ResumeDocument;
import com.recruit.agent.resume.model.ResumeDocumentStatus;
import com.recruit.agent.resume.repository.ResumeDocumentRepository;
import com.recruit.agent.resume.service.impl.ResumeApplicationServiceImpl;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class ResumeApplicationServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldUploadResumeAndCreateDocumentForExistingCandidate() throws IOException {
        CandidateRepository candidateRepository = org.mockito.Mockito.mock(CandidateRepository.class);
        ResumeDocumentRepository resumeDocumentRepository = org.mockito.Mockito.mock(ResumeDocumentRepository.class);
        ResumeIngestionService resumeIngestionService = org.mockito.Mockito.mock(ResumeIngestionService.class);
        SchoolTierResolver schoolTierResolver = org.mockito.Mockito.mock(SchoolTierResolver.class);

        ResumeApplicationServiceImpl service = new ResumeApplicationServiceImpl(
            candidateRepository,
            resumeDocumentRepository,
            resumeIngestionService,
            schoolTierResolver
        );
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());

        Candidate candidate = new Candidate();
        candidate.setId("candidate-1");
        candidate.setCandidateNo("C-001");
        candidate.setFullName("张三");
        candidate.setTotalYearsOfExperience(BigDecimal.valueOf(5));

        when(candidateRepository.findById("candidate-1")).thenReturn(Optional.of(candidate));
        when(schoolTierResolver.resolve(any(), any())).thenReturn(SchoolTier.GENERAL_UNDERGRAD);
        when(resumeDocumentRepository.findByCandidateIdAndActiveVersion("candidate-1", true)).thenReturn(Optional.empty());
        when(resumeDocumentRepository.findTopByCandidateIdOrderByVersionNoDesc("candidate-1")).thenReturn(Optional.empty());
        when(resumeDocumentRepository.save(any(ResumeDocument.class))).thenAnswer(invocation -> {
            ResumeDocument document = invocation.getArgument(0);
            if (document.getId() == null) {
                document.setId("doc-1");
            }
            if (document.getCreatedAt() == null) {
                document.setCreatedAt(java.time.LocalDateTime.now());
            }
            return document;
        });

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume.pdf",
            "application/pdf",
            "mock pdf content".getBytes()
        );

        ResumeUploadRequest request = new ResumeUploadRequest();
        request.setCandidateId("candidate-1");

        ResumeUploadResponse response = service.uploadResume(request, file);

        assertEquals("doc-1", response.getDocumentId());
        assertEquals("candidate-1", response.getCandidateId());
        assertEquals("resume.pdf", response.getFileName());
        assertEquals(1, response.getVersionNo());
        assertEquals(ResumeDocumentStatus.UPLOADED.name(), response.getStatus());
        assertNotNull(response.getFileStorageKey());
        assertTrue(Files.exists(Path.of(response.getFileStorageKey())));

        verify(resumeDocumentRepository).save(any(ResumeDocument.class));
        verify(resumeIngestionService).ingest(any(ResumeDocument.class), any(Path.class));
    }

    @Test
    void shouldCreateCandidateFromCandidateInfoWhenCandidateIdMissing() throws IOException {
        CandidateRepository candidateRepository = org.mockito.Mockito.mock(CandidateRepository.class);
        ResumeDocumentRepository resumeDocumentRepository = org.mockito.Mockito.mock(ResumeDocumentRepository.class);
        ResumeIngestionService resumeIngestionService = org.mockito.Mockito.mock(ResumeIngestionService.class);
        SchoolTierResolver schoolTierResolver = org.mockito.Mockito.mock(SchoolTierResolver.class);

        ResumeApplicationServiceImpl service = new ResumeApplicationServiceImpl(
            candidateRepository,
            resumeDocumentRepository,
            resumeIngestionService,
            schoolTierResolver
        );
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());

        when(schoolTierResolver.resolve("重庆科技大学", DegreeLevel.BACHELOR)).thenReturn(SchoolTier.GENERAL_UNDERGRAD);
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate candidate = invocation.getArgument(0);
            if (candidate.getId() == null) {
                candidate.setId("candidate-new-1");
            }
            return candidate;
        });
        when(resumeDocumentRepository.findByCandidateIdAndActiveVersion("candidate-new-1", true)).thenReturn(Optional.empty());
        when(resumeDocumentRepository.findTopByCandidateIdOrderByVersionNoDesc("candidate-new-1")).thenReturn(Optional.empty());
        when(resumeDocumentRepository.save(any(ResumeDocument.class))).thenAnswer(invocation -> {
            ResumeDocument document = invocation.getArgument(0);
            if (document.getId() == null) {
                document.setId("doc-2");
            }
            if (document.getCreatedAt() == null) {
                document.setCreatedAt(java.time.LocalDateTime.now());
            }
            return document;
        });

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume.pdf",
            "application/pdf",
            "mock pdf content".getBytes()
        );

        ResumeCandidateInfo candidateInfo = new ResumeCandidateInfo();
        candidateInfo.setFullName("李四");
        candidateInfo.setEmail("lisi@example.com");
        candidateInfo.setPhone("13800138000");
        candidateInfo.setCurrentCity("上海");
        candidateInfo.setSchoolName("重庆科技大学");
        candidateInfo.setHighestDegree(DegreeLevel.BACHELOR);
        candidateInfo.setTotalYearsOfExperience(BigDecimal.valueOf(2));
        candidateInfo.setSource(CandidateSource.HR_UPLOAD);

        ResumeUploadRequest request = new ResumeUploadRequest();
        request.setCandidateInfo(candidateInfo);

        ResumeUploadResponse response = service.uploadResume(request, file);

        assertEquals("doc-2", response.getDocumentId());
        assertEquals("candidate-new-1", response.getCandidateId());

        ArgumentCaptor<Candidate> candidateCaptor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository).save(candidateCaptor.capture());
        assertEquals(SchoolTier.GENERAL_UNDERGRAD, candidateCaptor.getValue().getSchoolTier());
        verify(resumeDocumentRepository).save(any(ResumeDocument.class));
        verify(resumeIngestionService).ingest(any(ResumeDocument.class), any(Path.class));
    }
}
