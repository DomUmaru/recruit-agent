package com.recruit.agent.interview.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 面试题证据。
 */
@Getter
@Setter
@NoArgsConstructor
public class InterviewQuestionEvidenceVO {

    private String section;

    private Integer page;

    private String content;
}
