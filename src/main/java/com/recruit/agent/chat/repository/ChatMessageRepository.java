package com.recruit.agent.chat.repository;

import com.recruit.agent.chat.model.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 聊天消息仓储接口。
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    /**
     * 按会话 ID 顺序查询消息列表。
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<ChatMessage> findBySessionIdOrderBySequenceNoAsc(String sessionId);
}
