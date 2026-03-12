package com.recruit.agent.search.rerank;

import com.recruit.agent.search.vo.CandidateSearchItemVO;
import java.util.List;

public interface CandidateSearchRerankService {

    List<CandidateSearchItemVO> rerank(String query, List<CandidateSearchItemVO> candidates);
}
