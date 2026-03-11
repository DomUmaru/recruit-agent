package com.recruit.agent.candidate.model;

import com.recruit.agent.common.model.BaseAuditEntity;
import com.recruit.agent.resume.model.ResumeDocument;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 候选人主数据实体，承载简历解析、检索召回和面试生成的基础信息。
 */
@Entity
@Table(name = "candidate")
@Getter
@Setter
@NoArgsConstructor
public class Candidate extends BaseAuditEntity {

    /**
     * 候选人编号，供业务侧唯一识别。
     */
    @Column(name = "candidate_no", nullable = false, unique = true, length = 64)
    private String candidateNo;

    /**
     * 候选人姓名。
     */
    @Column(name = "full_name", nullable = false, length = 128)
    private String fullName;

    /**
     * 联系电话。
     */
    @Column(name = "phone", length = 32)
    private String phone;

    /**
     * 邮箱地址。
     */
    @Column(name = "email", length = 128)
    private String email;

    /**
     * 当前所在城市。
     */
    @Column(name = "current_city", length = 64)
    private String currentCity;

    /**
     * 当前公司名称。
     */
    @Column(name = "current_company", length = 128)
    private String currentCompany;

    /**
     * 当前岗位名称。
     */
    @Column(name = "current_title", length = 128)
    private String currentTitle;

    /**
     * 总工作年限。
     */
    @Column(name = "total_years_of_experience", precision = 5, scale = 1)
    private BigDecimal totalYearsOfExperience;

    /**
     * 最高学历。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "highest_degree", length = 32)
    private DegreeLevel highestDegree;

    /**
     * 毕业院校名称。
     */
    @Column(name = "school_name", length = 256)
    private String schoolName;

    /**
     * 学校层级标签。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "school_tier", length = 32)
    private SchoolTier schoolTier;

    /**
     * 候选人来源渠道。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private CandidateSource source;

    /**
     * 候选人当前状态。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CandidateStatus status;

    /**
     * 是否具备大厂背景。
     */
    @Column(name = "is_big_tech", nullable = false)
    private boolean bigTech;

    /**
     * 是否为外包经历候选人。
     */
    @Column(name = "is_outsourcing", nullable = false)
    private boolean outsourcing;

    /**
     * 候选人摘要。
     */
    @Column(name = "summary", length = 2000)
    private String summary;

    /**
     * 候选人关联的简历文档列表。
     */
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ResumeDocument> resumeDocuments = new ArrayList<>();

}
