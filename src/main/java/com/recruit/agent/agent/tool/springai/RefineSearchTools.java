package com.recruit.agent.agent.tool.springai;

import com.recruit.agent.search.dto.CandidateSearchRefineRequest;
import com.recruit.agent.search.service.CandidateSearchRefinementService;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 搜索 refinement 工具集。
 */
@Component
public class RefineSearchTools {

    private final CandidateSearchRefinementService candidateSearchRefinementService;

    public RefineSearchTools(CandidateSearchRefinementService candidateSearchRefinementService) {
        this.candidateSearchRefinementService = candidateSearchRefinementService;
    }

    /**
     * 基于上一轮搜索结果追加或覆盖筛选条件，再次检索候选人。
     *
     * @param request refinement 请求
     * @return 搜索结果
     */
    @Tool(name = "refineSearchFilterTool", description = "基于上一轮搜索请求追加或覆盖筛选条件，执行 refinement 搜索。", returnDirect = true)
    public CandidateSearchResponse refineSearchFilterTool(CandidateSearchRefineRequest request) {
        return candidateSearchRefinementService.refineSearch(request);
    }
}
