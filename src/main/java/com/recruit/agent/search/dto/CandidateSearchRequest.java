package com.recruit.agent.search.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Candidate search request.
 */
@Getter
@Setter
@NoArgsConstructor
public class CandidateSearchRequest {

    /**
     * Bound position ID for JD-aware search and ranking.
     */
    private String positionId;

    /**
     * Natural language query or keyword query.
     */
    private String query;

    /**
     * Structured filter.
     */
    @Valid
    private CandidateSearchFilter filter = new CandidateSearchFilter();

    /**
     * Optional candidate scope for refinement or local rerank.
     */
    private List<String> scopeCandidateIds;

    /**
     * Number of returned candidates.
     */
    @Min(1)
    @Max(50)
    private Integer limit = 10;

    /**
     * Evidence count per candidate.
     */
    @Min(0)
    @Max(5)
    private Integer evidenceLimit = 3;
}
