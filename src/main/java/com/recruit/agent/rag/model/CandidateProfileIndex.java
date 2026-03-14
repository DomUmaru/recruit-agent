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

@Document(indexName = "candidate_profile")
@Getter
@Setter
@NoArgsConstructor
public class CandidateProfileIndex {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String candidateId;

    @Field(type = FieldType.Keyword)
    private String candidateNo;

    @Field(type = FieldType.Keyword)
    private String careerStage;

    @Field(type = FieldType.Text)
    private String fullName;

    @Field(type = FieldType.Keyword)
    private String currentCity;

    @Field(type = FieldType.Keyword)
    private String schoolName;

    @Field(type = FieldType.Keyword)
    private String highestDegree;

    @Field(type = FieldType.Keyword)
    private String schoolTier;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 10)
    private BigDecimal totalYearsOfExperience;

    @Field(type = FieldType.Keyword)
    private List<String> technicalSkills;

    @Field(type = FieldType.Keyword)
    private List<String> industryTags;

    @Field(type = FieldType.Keyword)
    private List<String> companyTags;

    @Field(type = FieldType.Keyword)
    private List<String> projectTags;

    @Field(type = FieldType.Boolean)
    private Boolean bigTech;

    @Field(type = FieldType.Boolean)
    private Boolean outsourcing;

    @Field(type = FieldType.Text)
    private String profileSummary;

    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;

    @Field(type = FieldType.Dense_Vector, dims = 1024)
    private float[] embedding;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss")
    private LocalDateTime indexedAt;
}
