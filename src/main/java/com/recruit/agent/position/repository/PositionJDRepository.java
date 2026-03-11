package com.recruit.agent.position.repository;

import com.recruit.agent.position.model.PositionJD;
import com.recruit.agent.position.model.PositionJDStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 岗位 JD 仓储接口。
 */
public interface PositionJDRepository extends JpaRepository<PositionJD, String> {

    /**
     * 按 JD 编号查询。
     *
     * @param jdNo JD 编号
     * @return JD 信息
     */
    Optional<PositionJD> findByJdNo(String jdNo);

    /**
     * 按状态查询岗位 JD 列表。
     *
     * @param status JD 状态
     * @return JD 列表
     */
    List<PositionJD> findByStatus(PositionJDStatus status);
}
