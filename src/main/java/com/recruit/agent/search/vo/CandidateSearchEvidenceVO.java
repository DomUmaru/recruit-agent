package com.recruit.agent.search.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 候选人证据片段视图。
 */
@Getter
@Setter
@NoArgsConstructor
public class CandidateSearchEvidenceVO {

    private String chunkId;

    private String docId;

    private String section;

    private Integer page;

    private String content;
}
