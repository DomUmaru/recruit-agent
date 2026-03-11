package com.recruit.agent.resume.controller;

import com.recruit.agent.resume.dto.ResumeUploadRequest;
import com.recruit.agent.resume.dto.ResumeUploadResponse;
import com.recruit.agent.resume.service.ResumeApplicationService;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历上传控制器。
 */
@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeApplicationService resumeApplicationService;

    public ResumeController(ResumeApplicationService resumeApplicationService) {
        this.resumeApplicationService = resumeApplicationService;
    }

    /**
     * 上传简历文件并创建文档记录。
     *
     * @param request 上传请求
     * @param file 简历文件
     * @return 上传结果
     * @throws IOException 文件写入异常
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeUploadResponse upload(@Valid @ModelAttribute ResumeUploadRequest request,
                                       @RequestPart("file") MultipartFile file) throws IOException {
        return resumeApplicationService.uploadResume(request, file);
    }
}
