package com.recruit.agent.search.parser;

import com.recruit.agent.search.dto.CandidateSearchFilter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SearchIntentParseResult {

    private String residualQuery;

    private CandidateSearchFilter filter = new CandidateSearchFilter();
}
