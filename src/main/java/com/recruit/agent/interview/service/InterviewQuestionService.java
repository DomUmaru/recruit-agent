package com.recruit.agent.interview.service;

import com.recruit.agent.interview.dto.InterviewQuestionRequest;
import com.recruit.agent.interview.vo.InterviewQuestionResponse;

/**
 * 面试题生成服务。
 */
public interface InterviewQuestionService {

    InterviewQuestionResponse generate(InterviewQuestionRequest request);
}
