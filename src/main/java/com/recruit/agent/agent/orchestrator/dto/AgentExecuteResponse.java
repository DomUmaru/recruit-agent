package com.recruit.agent.agent.orchestrator.dto;

import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.comparison.vo.CandidateComparisonResponse;
import com.recruit.agent.interview.vo.InterviewQuestionResponse;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Agent 执行响应。
 */
@Getter
@Setter
@NoArgsConstructor
public class AgentExecuteResponse {

    private String sessionNo;

    private AgentRouteDecision routeDecision;

    private CandidateSearchResponse searchResponse;

    private CandidateComparisonResponse comparisonResponse;

    private InterviewQuestionResponse interviewResponse;

    private String summary;
}
