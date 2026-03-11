package com.recruit.agent.resume.model;

/**
 * 简历解析路径枚举，用于区分 PDFBox 与 OCR 等处理方式。
 */
public enum ResumeParseType {
    PDF_TEXT,
    OCR_SCANNED,
    MIXED,
    UNKNOWN
}
