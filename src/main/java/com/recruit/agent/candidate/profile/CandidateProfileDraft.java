package com.recruit.agent.candidate.profile;

import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 候选人画像草稿对象。
 */
@Setter
@Getter
public class CandidateProfileDraft {

    /**
     * 学历层级。
     */
    private DegreeLevel highestDegree;

    /**
     * 学校名称。
     */
    private String schoolName;

    /**
     * 学校层级。
     */
    private SchoolTier schoolTier;

    /**
     * 工作年限。
     */
    private BigDecimal totalYearsOfExperience;

    /**
     * 技术栈标签。
     */
    private List<String> technicalSkills;

    /**
     * 行业标签。
     */
    private List<String> industryTags;

    /**
     * 公司标签。
     */
    private List<String> companyTags;

    /**
     * 项目标签。
     */
    private List<String> projectTags;

    /**
     * 是否具备大厂背景。
     */
    private Boolean bigTech;

    /**
     * 是否包含外包标签。
     */
    private Boolean outsourcing;

    /**
     * 候选人摘要。
     */
    private String profileSummary;

    /**
     * 扩展元数据。
     */
    private Map<String, Object> metadata;

}
