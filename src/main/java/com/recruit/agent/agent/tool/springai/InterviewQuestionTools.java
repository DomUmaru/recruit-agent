package com.recruit.agent.agent.tool.springai;

import com.recruit.agent.interview.dto.InterviewQuestionRequest;
import com.recruit.agent.interview.service.InterviewQuestionService;
import com.recruit.agent.interview.vo.InterviewQuestionResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 面试题工具集。
 */
@Component
public class InterviewQuestionTools {

    private final InterviewQuestionService interviewQuestionService;

    public InterviewQuestionTools(InterviewQuestionService interviewQuestionService) {
        this.interviewQuestionService = interviewQuestionService;
    }

    @Tool(name = "generateInterviewQuestionsTool", description = "基于候选人画像和简历证据生成结构化面试题，返回每位候选人的问题、追问方向与证据。", returnDirect = true)
    public InterviewQuestionResponse generateInterviewQuestionsTool(InterviewQuestionRequest request) {
        return interviewQuestionService.generate(request);
    }
}
