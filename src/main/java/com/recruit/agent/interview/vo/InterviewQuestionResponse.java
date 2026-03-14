package com.recruit.agent.interview.vo;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 面试题生成响应。
 */
@Getter
@Setter
@NoArgsConstructor
public class InterviewQuestionResponse {

    private String targetQuery;

    private List<CandidateInterviewQuestionVO> candidates;

    private String summary;
}
