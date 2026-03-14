package com.recruit.agent.comparison.controller;

import com.recruit.agent.comparison.dto.CandidateComparisonRequest;
import com.recruit.agent.comparison.service.CandidateComparisonService;
import com.recruit.agent.comparison.vo.CandidateComparisonResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 候选人对比控制器。
 */
@RestController
@RequestMapping("/api/comparison")
public class CandidateComparisonController {

    private final CandidateComparisonService candidateComparisonService;

    public CandidateComparisonController(CandidateComparisonService candidateComparisonService) {
        this.candidateComparisonService = candidateComparisonService;
    }

    /**
     * 对多个候选人做结构化对比。
     *
     * @param request 对比请求
     * @return 对比结果
     */
    @PostMapping(value = "/candidates", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CandidateComparisonResponse compare(@Valid @RequestBody CandidateComparisonRequest request) {
        return candidateComparisonService.compare(request);
    }
}
