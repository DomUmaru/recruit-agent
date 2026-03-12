package com.recruit.agent.search.controller;

import com.recruit.agent.search.dto.CandidateSearchRefineRequest;
import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.service.CandidateSearchRefinementService;
import com.recruit.agent.search.service.CandidateSearchService;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 候选人搜索控制器。
 */
@RestController
@RequestMapping("/api/search")
public class CandidateSearchController {

    private final CandidateSearchService candidateSearchService;
    private final CandidateSearchRefinementService candidateSearchRefinementService;

    public CandidateSearchController(CandidateSearchService candidateSearchService,
                                     CandidateSearchRefinementService candidateSearchRefinementService) {
        this.candidateSearchService = candidateSearchService;
        this.candidateSearchRefinementService = candidateSearchRefinementService;
    }

    /**
     * 搜索候选人并返回证据片段。
     *
     * @param request 搜索请求
     * @return 搜索结果
     */
    @PostMapping(value = "/candidates", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CandidateSearchResponse search(@Valid @RequestBody CandidateSearchRequest request) {
        return candidateSearchService.search(request);
    }

    /**
     * 在上一轮搜索请求基础上追加或覆盖筛选条件后重新搜索。
     *
     * @param request refinement 请求
     * @return 搜索结果
     */
    @PostMapping(value = "/candidates/refine", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CandidateSearchResponse refine(@Valid @RequestBody CandidateSearchRefineRequest request) {
        return candidateSearchRefinementService.refineSearch(request);
    }
}
