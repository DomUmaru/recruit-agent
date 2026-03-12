package com.recruit.agent.search.dto;

import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 候选人搜索过滤条件。
 */
@Getter
@Setter
@NoArgsConstructor
public class CandidateSearchFilter {

    /**
     * 最高学历过滤条件。
     */
    private List<DegreeLevel> highestDegrees;

    /**
     * 学校层级过滤条件。
     */
    private List<SchoolTier> schoolTiers;

    /**
     * 最低工作年限。
     */
    private BigDecimal minYearsOfExperience;

    /**
     * 技术栈过滤条件。
     */
    private List<String> technicalSkills;

    /**
     * 当前城市过滤条件。
     */
    private String currentCity;

    /**
     * 是否要求大厂背景。
     */
    private Boolean bigTech;

    /**
     * 是否要求外包标签。
     */
    private Boolean outsourcing;
}
