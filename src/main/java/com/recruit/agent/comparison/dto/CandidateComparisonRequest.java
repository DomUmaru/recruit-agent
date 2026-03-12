package com.recruit.agent.comparison.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 候选人对比请求。
 */
@Getter
@Setter
@NoArgsConstructor
public class CandidateComparisonRequest {

    /**
     * 待对比候选人 ID 列表。
     */
    @NotEmpty
    private List<String> candidateIds;

    /**
     * 可选岗位查询语句，用于生成契合度说明。
     */
    private String targetQuery;
}
