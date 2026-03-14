package com.recruit.agent.rag.model;

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

@Document(indexName = "resume_chunk")
@Getter
@Setter
@NoArgsConstructor
public class ResumeChunk {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String candidateId;

    @Field(type = FieldType.Keyword)
    private String docId;

    @Field(type = FieldType.Keyword)
    private String parentId;

    @Field(type = FieldType.Keyword)
    private String chunkType;

    @Field(type = FieldType.Keyword)
    private String section;

    @Field(type = FieldType.Text)
    private String subSectionTitle;

    @Field(type = FieldType.Integer)
    private Integer page;

    @Field(type = FieldType.Integer)
    private Integer chunkOrder;

    @Field(type = FieldType.Text)
    private String content;

    @Field(type = FieldType.Text)
    private String normalizedContent;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;

    @Field(type = FieldType.Dense_Vector, dims = 1024)
    private float[] embedding;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss")
    private LocalDateTime indexedAt;
}
