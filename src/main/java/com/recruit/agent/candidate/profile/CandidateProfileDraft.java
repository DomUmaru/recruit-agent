package com.recruit.agent.candidate.profile;

import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 候选人画像草稿对象。
 */
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

    public DegreeLevel getHighestDegree() {
        return highestDegree;
    }

    public void setHighestDegree(DegreeLevel highestDegree) {
        this.highestDegree = highestDegree;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public SchoolTier getSchoolTier() {
        return schoolTier;
    }

    public void setSchoolTier(SchoolTier schoolTier) {
        this.schoolTier = schoolTier;
    }

    public BigDecimal getTotalYearsOfExperience() {
        return totalYearsOfExperience;
    }

    public void setTotalYearsOfExperience(BigDecimal totalYearsOfExperience) {
        this.totalYearsOfExperience = totalYearsOfExperience;
    }

    public List<String> getTechnicalSkills() {
        return technicalSkills;
    }

    public void setTechnicalSkills(List<String> technicalSkills) {
        this.technicalSkills = technicalSkills;
    }

    public List<String> getIndustryTags() {
        return industryTags;
    }

    public void setIndustryTags(List<String> industryTags) {
        this.industryTags = industryTags;
    }

    public List<String> getCompanyTags() {
        return companyTags;
    }

    public void setCompanyTags(List<String> companyTags) {
        this.companyTags = companyTags;
    }

    public List<String> getProjectTags() {
        return projectTags;
    }

    public void setProjectTags(List<String> projectTags) {
        this.projectTags = projectTags;
    }

    public Boolean getBigTech() {
        return bigTech;
    }

    public void setBigTech(Boolean bigTech) {
        this.bigTech = bigTech;
    }

    public Boolean getOutsourcing() {
        return outsourcing;
    }

    public void setOutsourcing(Boolean outsourcing) {
        this.outsourcing = outsourcing;
    }

    public String getProfileSummary() {
        return profileSummary;
    }

    public void setProfileSummary(String profileSummary) {
        this.profileSummary = profileSummary;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
