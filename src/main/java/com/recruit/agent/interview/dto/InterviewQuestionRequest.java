package com.recruit.agent.interview.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 面试题生成请求。
 */
@Getter
@Setter
@NoArgsConstructor
public class InterviewQuestionRequest {

    private List<String> candidateIds;

    private String positionId;

    private String targetQuery;
}
