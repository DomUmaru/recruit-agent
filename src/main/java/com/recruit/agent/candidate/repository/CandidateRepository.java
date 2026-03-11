package com.recruit.agent.candidate.repository;

import com.recruit.agent.candidate.model.Candidate;
import com.recruit.agent.candidate.model.CandidateStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 候选人主数据仓储接口。
 */
public interface CandidateRepository extends JpaRepository<Candidate, String> {

    /**
     * 按候选人编号查询。
     *
     * @param candidateNo 候选人编号
     * @return 候选人信息
     */
    Optional<Candidate> findByCandidateNo(String candidateNo);

    /**
     * 按状态查询候选人列表。
     *
     * @param status 候选人状态
     * @return 候选人列表
     */
    List<Candidate> findByStatus(CandidateStatus status);
}
