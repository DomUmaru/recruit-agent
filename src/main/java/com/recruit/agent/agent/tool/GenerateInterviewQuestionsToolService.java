package com.recruit.agent.agent.tool;

import com.recruit.agent.interview.dto.InterviewQuestionRequest;
import com.recruit.agent.interview.vo.InterviewQuestionResponse;

/**
 * 面试题生成工具服务。
 */
public interface GenerateInterviewQuestionsToolService {

    InterviewQuestionResponse execute(InterviewQuestionRequest request);
}
