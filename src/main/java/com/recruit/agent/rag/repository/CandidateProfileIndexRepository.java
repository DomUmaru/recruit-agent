package com.recruit.agent.rag.repository;

import com.recruit.agent.rag.model.CandidateProfileIndex;
import java.util.List;
import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 候选人画像索引仓储接口。
 */
public interface CandidateProfileIndexRepository extends ElasticsearchRepository<CandidateProfileIndex, String> {

    /**
     * 按候选人 ID 查询画像索引。
     *
     * @param candidateId 候选人 ID
     * @return 画像索引
     */
    Optional<CandidateProfileIndex> findByCandidateId(String candidateId);

    /**
     * 按城市查询画像索引列表。
     *
     * @param currentCity 当前城市
     * @return 画像索引列表
     */
    List<CandidateProfileIndex> findByCurrentCity(String currentCity);
}
