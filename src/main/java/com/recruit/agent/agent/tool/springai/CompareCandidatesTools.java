package com.recruit.agent.agent.tool.springai;

import com.recruit.agent.comparison.dto.CandidateComparisonRequest;
import com.recruit.agent.comparison.service.CandidateComparisonService;
import com.recruit.agent.comparison.vo.CandidateComparisonResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 候选人对比工具集。
 */
@Component
public class CompareCandidatesTools {

    private final CandidateComparisonService candidateComparisonService;

    public CompareCandidatesTools(CandidateComparisonService candidateComparisonService) {
        this.candidateComparisonService = candidateComparisonService;
    }

    /**
     * 对多个候选人进行结构化对比。
     *
     * @param request 对比请求
     * @return 对比结果
     */
    @Tool(name = "compareCandidatesTool", description = "对多个候选人进行结构化对比，并返回亮点、风险点与证据。", returnDirect = true)
    public CandidateComparisonResponse compareCandidatesTool(CandidateComparisonRequest request) {
        return candidateComparisonService.compare(request);
    }
}
