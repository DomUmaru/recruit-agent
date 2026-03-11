package com.recruit.agent.resume.model;

import com.recruit.agent.candidate.model.Candidate;
import com.recruit.agent.common.model.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 简历文档实体，记录上传文件及解析产物。
 */
@Entity
@Table(name = "resume_document")
@Getter
@Setter
@NoArgsConstructor
public class ResumeDocument extends BaseAuditEntity {

    /**
     * 所属候选人。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    /**
     * 原始文件名。
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * 文件存储路径或对象存储 Key。
     */
    @Column(name = "file_storage_key", nullable = false, length = 512)
    private String fileStorageKey;

    /**
     * 文件校验值，用于去重与幂等。
     */
    @Column(name = "file_checksum", length = 128)
    private String fileChecksum;

    /**
     * 简历版本号。
     */
    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    /**
     * 文档总页数。
     */
    @Column(name = "page_count")
    private Integer pageCount;

    /**
     * 简历处理状态。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ResumeDocumentStatus status;

    /**
     * 实际采用的解析方式。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "parse_type", length = 32)
    private ResumeParseType parseType;

    /**
     * 是否为当前生效版本。
     */
    @Column(name = "active_version", nullable = false)
    private boolean activeVersion;

    /**
     * 原始提取文本。
     */
    @Lob
    @Column(name = "raw_text")
    private String rawText;

    /**
     * 清洗后的标准化文本。
     */
    @Lob
    @Column(name = "cleaned_text")
    private String cleanedText;

    /**
     * 按页文本内容，建议保存为 JSON。
     */
    @Lob
    @Column(name = "page_text_json")
    private String pageTextJson;

    /**
     * 解析过程日志，便于排障。
     */
    @Lob
    @Column(name = "parser_log")
    private String parserLog;

}
