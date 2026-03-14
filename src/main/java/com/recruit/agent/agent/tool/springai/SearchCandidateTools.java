package com.recruit.agent.agent.tool.springai;

import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.service.CandidateSearchService;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 候选人搜索工具集。
 */
@Component
public class SearchCandidateTools {

    private final CandidateSearchService candidateSearchService;

    public SearchCandidateTools(CandidateSearchService candidateSearchService) {
        this.candidateSearchService = candidateSearchService;
    }

    /**
     * 根据自然语言招聘需求和结构化筛选条件搜索候选人。
     *
     * @param request 搜索请求
     * @return 搜索结果
     */
    @Tool(name = "searchCandidateByJDTool", description = "根据招聘需求搜索候选人，并返回匹配结果与证据片段。", returnDirect = true)
    public CandidateSearchResponse searchCandidateByJDTool(CandidateSearchRequest request) {
        return candidateSearchService.search(request);
    }
}
