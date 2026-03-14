package com.recruit.agent.agent.execution;

import com.recruit.agent.comparison.vo.CandidateComparisonResponse;
import com.recruit.agent.interview.vo.InterviewQuestionResponse;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Agent 工具执行结果。
 */
@Getter
@Setter
@NoArgsConstructor
public class AgentToolExecutionResult {

    private CandidateSearchResponse searchResponse;

    private CandidateComparisonResponse comparisonResponse;

    private InterviewQuestionResponse interviewResponse;
}
