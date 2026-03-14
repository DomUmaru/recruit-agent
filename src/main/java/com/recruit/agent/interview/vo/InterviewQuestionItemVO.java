package com.recruit.agent.interview.vo;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 单道面试题。
 */
@Getter
@Setter
@NoArgsConstructor
public class InterviewQuestionItemVO {

    private String category;

    private String question;

    private String rationale;

    private List<InterviewQuestionEvidenceVO> evidenceList;
}
