package com.recruit.agent.resume.controller;

import com.recruit.agent.resume.dto.ResumeCandidateInfo;
import com.recruit.agent.resume.dto.ResumeUploadRequest;
import com.recruit.agent.resume.dto.ResumeUploadResponse;
import com.recruit.agent.resume.service.ResumeApplicationService;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeApplicationService resumeApplicationService;

    public ResumeController(ResumeApplicationService resumeApplicationService) {
        this.resumeApplicationService = resumeApplicationService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeUploadResponse upload(@RequestParam(value = "candidateId", required = false) String candidateId,
                                       @RequestPart(value = "candidateInfo", required = false) @Valid ResumeCandidateInfo candidateInfo,
                                       @RequestPart("file") MultipartFile file) throws IOException {
        ResumeUploadRequest request = new ResumeUploadRequest();
        request.setCandidateId(candidateId);
        request.setCandidateInfo(candidateInfo);
        return resumeApplicationService.uploadResume(request, file);
    }
}
