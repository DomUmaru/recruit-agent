package com.recruit.agent.resume.dto;

import java.time.LocalDateTime;

/**
 * 简历上传响应对象。
 */
public class ResumeUploadResponse {

    /**
     * 简历文档 ID。
     */
    private String documentId;

    /**
     * 候选人 ID。
     */
    private String candidateId;

    /**
     * 文件名。
     */
    private String fileName;

    /**
     * 简历版本号。
     */
    private Integer versionNo;

    /**
     * 简历状态。
     */
    private String status;

    /**
     * 文件存储路径。
     */
    private String fileStorageKey;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFileStorageKey() {
        return fileStorageKey;
    }

    public void setFileStorageKey(String fileStorageKey) {
        this.fileStorageKey = fileStorageKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
