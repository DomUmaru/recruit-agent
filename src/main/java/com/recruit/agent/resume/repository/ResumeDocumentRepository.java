package com.recruit.agent.resume.repository;

import com.recruit.agent.resume.model.ResumeDocument;
import com.recruit.agent.resume.model.ResumeDocumentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 简历文档仓储接口。
 */
public interface ResumeDocumentRepository extends JpaRepository<ResumeDocument, String> {

    /**
     * 按候选人 ID 查询简历列表。
     *
     * @param candidateId 候选人 ID
     * @return 简历文档列表
     */
    List<ResumeDocument> findByCandidateIdOrderByVersionNoDesc(String candidateId);

    /**
     * 查询候选人的当前生效版本简历。
     *
     * @param candidateId 候选人 ID
     * @param activeVersion 是否当前版本
     * @return 简历文档
     */
    Optional<ResumeDocument> findByCandidateIdAndActiveVersion(String candidateId, boolean activeVersion);

    /**
     * 查询候选人的最新版本简历。
     *
     * @param candidateId 候选人 ID
     * @return 最新版本简历
     */
    Optional<ResumeDocument> findTopByCandidateIdOrderByVersionNoDesc(String candidateId);

    /**
     * 按处理状态查询简历列表。
     *
     * @param status 简历处理状态
     * @return 简历文档列表
     */
    List<ResumeDocument> findByStatus(ResumeDocumentStatus status);
}
