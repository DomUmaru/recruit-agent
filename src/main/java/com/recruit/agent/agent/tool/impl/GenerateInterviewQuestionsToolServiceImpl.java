package com.recruit.agent.agent.tool.impl;

import com.recruit.agent.agent.tool.GenerateInterviewQuestionsToolService;
import com.recruit.agent.interview.dto.InterviewQuestionRequest;
import com.recruit.agent.interview.service.InterviewQuestionService;
import com.recruit.agent.interview.vo.InterviewQuestionResponse;
import org.springframework.stereotype.Service;

/**
 * 面试题生成工具服务实现。
 */
@Service
public class GenerateInterviewQuestionsToolServiceImpl implements GenerateInterviewQuestionsToolService {

    private final InterviewQuestionService interviewQuestionService;

    public GenerateInterviewQuestionsToolServiceImpl(InterviewQuestionService interviewQuestionService) {
        this.interviewQuestionService = interviewQuestionService;
    }

    @Override
    public InterviewQuestionResponse execute(InterviewQuestionRequest request) {
        return interviewQuestionService.generate(request);
    }
}
