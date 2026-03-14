package com.recruit.agent.resume.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumeUploadRequest {

    private String candidateId;

    private ResumeCandidateInfo candidateInfo;
}
