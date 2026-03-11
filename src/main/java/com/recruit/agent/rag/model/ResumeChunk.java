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

/**
 * 简历证据分块索引，用于细粒度检索与引用生成。
 */
@Document(indexName = "resume_chunk")
@Getter
@Setter
@NoArgsConstructor
public class ResumeChunk {

    /**
     * 分块文档主键。
     */
    @Id
    private String id;

    /**
     * 候选人 ID。
     */
    @Field(type = FieldType.Keyword)
    private String candidateId;

    /**
     * 简历文档 ID。
     */
    @Field(type = FieldType.Keyword)
    private String docId;

    /**
     * 父分块 ID，用于 Parent-Child Chunk 结构。
     */
    @Field(type = FieldType.Keyword)
    private String parentId;

    /**
     * 分块类型，如 parent 或 child。
     */
    @Field(type = FieldType.Keyword)
    private String chunkType;

    /**
     * 简历分段名称，如教育经历、项目经历。
     */
    @Field(type = FieldType.Keyword)
    private String section;

    /**
     * 所属页码。
     */
    @Field(type = FieldType.Integer)
    private Integer page;

    /**
     * 分块在文档中的顺序。
     */
    @Field(type = FieldType.Integer)
    private Integer chunkOrder;

    /**
     * 原始分块内容。
     */
    @Field(type = FieldType.Text)
    private String content;

    /**
     * 标准化后的分块内容。
     */
    @Field(type = FieldType.Text)
    private String normalizedContent;

    /**
     * 分块标签列表。
     */
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    /**
     * 扩展元数据。
     */
    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;

    /**
     * 向量字段，用于语义检索。
     */
    @Field(type = FieldType.Dense_Vector, dims = 1024)
    private float[] embedding;

    /**
     * 索引写入时间。
     */
    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss")
    private LocalDateTime indexedAt;

}
