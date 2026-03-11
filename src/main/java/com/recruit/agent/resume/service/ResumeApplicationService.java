package com.recruit.agent.resume.service;

import com.recruit.agent.resume.dto.ResumeUploadRequest;
import com.recruit.agent.resume.dto.ResumeUploadResponse;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历应用服务接口。
 */
public interface ResumeApplicationService {

    /**
     * 上传简历并创建文档记录。
     *
     * @param request 上传请求
     * @param file 上传文件
     * @return 上传响应
     * @throws IOException 文件写入异常
     */
    ResumeUploadResponse uploadResume(ResumeUploadRequest request, MultipartFile file) throws IOException;
}
