package com.recruit.agent.rag.repository;

import com.recruit.agent.rag.model.ResumeChunk;
import java.util.List;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 简历证据分块索引仓储接口。
 */
public interface ResumeChunkRepository extends ElasticsearchRepository<ResumeChunk, String> {

    /**
     * 按候选人 ID 查询分块列表。
     *
     * @param candidateId 候选人 ID
     * @return 分块列表
     */
    List<ResumeChunk> findByCandidateId(String candidateId);

    /**
     * 按简历文档 ID 查询分块列表。
     *
     * @param docId 简历文档 ID
     * @return 分块列表
     */
    List<ResumeChunk> findByDocId(String docId);
}
