package com.recruit.agent.resume.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruit.agent.candidate.model.Candidate;
import com.recruit.agent.candidate.repository.CandidateRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class ResumeApplicationServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldUploadResumeAndCreateDocument() throws IOException {
        CandidateRepository candidateRepository = org.mockito.Mockito.mock(CandidateRepository.class);
        ResumeDocumentRepository resumeDocumentRepository = org.mockito.Mockito.mock(ResumeDocumentRepository.class);
        ResumeIngestionService resumeIngestionService = org.mockito.Mockito.mock(ResumeIngestionService.class);

        ResumeApplicationServiceImpl service = new ResumeApplicationServiceImpl(
            candidateRepository,
            resumeDocumentRepository,
            resumeIngestionService
        );
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());

        Candidate candidate = new Candidate();
        candidate.setId("candidate-1");
        candidate.setCandidateNo("C-001");
        candidate.setFullName("张三");
        candidate.setTotalYearsOfExperience(BigDecimal.valueOf(5));

        when(candidateRepository.findById("candidate-1")).thenReturn(Optional.of(candidate));
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
}
