package com.recruit.agent.interview.vo;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 候选人面试题集。
 */
@Getter
@Setter
@NoArgsConstructor
public class CandidateInterviewQuestionVO {

    private String candidateId;

    private String candidateNo;

    private String fullName;

    private List<InterviewQuestionItemVO> questions;
}
