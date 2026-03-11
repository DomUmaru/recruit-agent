package com.recruit.agent.position.model;

import com.recruit.agent.common.model.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 岗位 JD 实体，同时保存原始文本与结构化解析结果。
 */
@Entity
@Table(name = "position_jd")
@Getter
@Setter
@NoArgsConstructor
public class PositionJD extends BaseAuditEntity {

    /**
     * JD 编号。
     */
    @Column(name = "jd_no", nullable = false, unique = true, length = 64)
    private String jdNo;

    /**
     * 岗位名称。
     */
    @Column(name = "title", nullable = false, length = 128)
    private String title;

    /**
     * 所属部门。
     */
    @Column(name = "department", length = 128)
    private String department;

    /**
     * 工作地点。
     */
    @Column(name = "location", length = 128)
    private String location;

    /**
     * 最低经验要求。
     */
    @Column(name = "min_years_of_experience")
    private Integer minYearsOfExperience;

    /**
     * 最高经验要求。
     */
    @Column(name = "max_years_of_experience")
    private Integer maxYearsOfExperience;

    /**
     * 学历要求。
     */
    @Column(name = "required_degree", length = 64)
    private String requiredDegree;

    /**
     * 核心技能要求，当前以文本串形式保存。
     */
    @Column(name = "priority_skills", length = 1000)
    private String prioritySkills;

    /**
     * 加分项技能，当前以文本串形式保存。
     */
    @Column(name = "bonus_skills", length = 1000)
    private String bonusSkills;

    /**
     * JD 原始文本。
     */
    @Lob
    @Column(name = "raw_jd_text", nullable = false)
    private String rawJdText;

    /**
     * 结构化 JD 结果，建议保存为 JSON。
     */
    @Lob
    @Column(name = "structured_jd_json")
    private String structuredJdJson;

    /**
     * Query 扩展词结果，建议保存为 JSON。
     */
    @Lob
    @Column(name = "query_expansion_json")
    private String queryExpansionJson;

    /**
     * JD 当前状态。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PositionJDStatus status;

}
