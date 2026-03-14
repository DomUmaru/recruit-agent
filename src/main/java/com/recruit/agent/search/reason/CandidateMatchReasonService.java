package com.recruit.agent.search.reason;

import com.recruit.agent.rag.model.CandidateProfileIndex;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import java.util.List;

public interface CandidateMatchReasonService {

    List<String> buildMatchReasons(CandidateProfileIndex profile,
                                   String query,
                                   List<String> queryTerms,
                                   CandidateSearchFilter filter);
}
