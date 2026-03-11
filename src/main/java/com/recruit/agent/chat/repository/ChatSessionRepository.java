package com.recruit.agent.chat.repository;

import com.recruit.agent.chat.model.ChatSession;
import com.recruit.agent.chat.model.ChatSessionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 聊天会话仓储接口。
 */
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    /**
     * 按会话编号查询。
     *
     * @param sessionNo 会话编号
     * @return 会话信息
     */
    Optional<ChatSession> findBySessionNo(String sessionNo);

    /**
     * 按用户 ID 和状态查询会话列表。
     *
     * @param userId 用户 ID
     * @param status 会话状态
     * @return 会话列表
     */
    List<ChatSession> findByUserIdAndStatusOrderByUpdatedAtDesc(String userId, ChatSessionStatus status);
}
