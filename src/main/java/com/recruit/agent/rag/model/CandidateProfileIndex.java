package com.recruit.agent.rag.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * 候选人画像聚合索引，用于粗召回与结构化过滤。
 */
@Document(indexName = "candidate_profile")
@Getter
@Setter
@NoArgsConstructor
public class CandidateProfileIndex {

    /**
     * 索引文档主键。
     */
    @Id
    private String id;

    /**
     * 候选人 ID。
     */
    @Field(type = FieldType.Keyword)
    private String candidateId;

    /**
     * 候选人编号。
     */
    @Field(type = FieldType.Keyword)
    private String candidateNo;

    /**
     * 候选人姓名。
     */
    @Field(type = FieldType.Text)
    private String fullName;

    /**
     * 当前所在城市。
     */
    @Field(type = FieldType.Keyword)
    private String currentCity;

    /**
     * 学校名称。
     */
    @Field(type = FieldType.Keyword)
    private String schoolName;

    /**
     * 最高学历。
     */
    @Field(type = FieldType.Keyword)
    private String highestDegree;

    /**
     * 学校层级标签。
     */
    @Field(type = FieldType.Keyword)
    private String schoolTier;

    /**
     * 总工作年限。
     */
    @Field(type = FieldType.Scaled_Float, scalingFactor = 10)
    private BigDecimal totalYearsOfExperience;

    /**
     * 技术栈标签列表。
     */
    @Field(type = FieldType.Keyword)
    private List<String> technicalSkills;

    /**
     * 行业标签列表。
     */
    @Field(type = FieldType.Keyword)
    private List<String> industryTags;

    /**
     * 公司标签列表。
     */
    @Field(type = FieldType.Keyword)
    private List<String> companyTags;

    /**
     * 项目标签列表。
     */
    @Field(type = FieldType.Keyword)
    private List<String> projectTags;

    /**
     * 是否具备大厂背景。
     */
    @Field(type = FieldType.Boolean)
    private Boolean bigTech;

    /**
     * 是否包含外包标签。
     */
    @Field(type = FieldType.Boolean)
    private Boolean outsourcing;

    /**
     * 候选人画像摘要。
     */
    @Field(type = FieldType.Text)
    private String profileSummary;

    /**
     * 扩展元数据。
     */
    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;

    /**
     * 向量字段，用于语义召回。
     */
    @Field(type = FieldType.Dense_Vector, dims = 1024)
    private float[] embedding;

    /**
     * 索引写入时间。
     */
    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss")
    private LocalDateTime indexedAt;

}
