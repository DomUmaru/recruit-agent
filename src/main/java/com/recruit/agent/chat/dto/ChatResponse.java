package com.recruit.agent.chat.dto;

import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.comparison.vo.CandidateComparisonResponse;
import com.recruit.agent.interview.vo.InterviewQuestionResponse;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 统一聊天响应。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatResponse {

    private String sessionNo;

    private ChatScene scene;

    private String toolName;

    private String summary;

    private CandidateSearchResponse searchResponse;

    private CandidateComparisonResponse comparisonResponse;

    private ChatComparisonPayload comparison;

    private InterviewQuestionResponse interviewResponse;
}
