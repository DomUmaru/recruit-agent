package com.recruit.agent.resume.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 简历上传请求对象。
 */
@Setter
@Getter
public class ResumeUploadRequest {

    /**
     * 候选人 ID。
     */
    @NotBlank(message = "候选人 ID 不能为空")
    private String candidateId;

}
