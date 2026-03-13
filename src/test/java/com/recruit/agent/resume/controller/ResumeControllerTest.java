package com.recruit.agent.resume.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruit.agent.resume.dto.ResumeUploadResponse;
import com.recruit.agent.resume.service.ResumeApplicationService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ResumeController.class)
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumeApplicationService resumeApplicationService;

    @Test
    void shouldUploadResumeWithExistingCandidateId() throws Exception {
        ResumeUploadResponse response = buildResponse("doc-1", "candidate-1");
        when(resumeApplicationService.uploadResume(any(), any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume.pdf",
            "application/pdf",
            "mock pdf".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/resumes/upload")
                .file(file)
                .param("candidateId", "candidate-1"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.documentId").value("doc-1"))
            .andExpect(jsonPath("$.candidateId").value("candidate-1"));
    }

    @Test
    void shouldUploadResumeWithCandidateInfoPart() throws Exception {
        ResumeUploadResponse response = buildResponse("doc-2", "candidate-2");
        when(resumeApplicationService.uploadResume(any(), any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "resume.pdf",
            "application/pdf",
            "mock pdf".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile candidateInfo = new MockMultipartFile(
            "candidateInfo",
            "",
            "application/json",
            """
                {
                  "fullName":"张三",
                  "email":"zhangsan@example.com",
                  "schoolName":"重庆科技大学"
                }
                """.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/resumes/upload")
                .file(file)
                .file(candidateInfo))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.documentId").value("doc-2"))
            .andExpect(jsonPath("$.candidateId").value("candidate-2"));
    }

    private ResumeUploadResponse buildResponse(String documentId, String candidateId) {
        ResumeUploadResponse response = new ResumeUploadResponse();
        response.setDocumentId(documentId);
        response.setCandidateId(candidateId);
        response.setFileName("resume.pdf");
        response.setVersionNo(1);
        response.setStatus("UPLOADED");
        response.setFileStorageKey("data/uploads/resumes/" + candidateId + "/resume.pdf");
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }
}
