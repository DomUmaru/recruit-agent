package com.recruit.agent.chat.model;

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
 * 聊天消息实体，用于历史回放与链路追踪。
 */
@Entity
@Table(name = "chat_message")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage extends BaseAuditEntity {

    /**
     * 所属会话。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    /**
     * 消息角色。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private ChatMessageRole role;

    /**
     * 消息顺序号。
     */
    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    /**
     * 消息正文。
     */
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    /**
     * 引用信息，建议保存为 JSON。
     */
    @Lob
    @Column(name = "citations_json")
    private String citationsJson;

    /**
     * 工具调用载荷，建议保存为 JSON。
     */
    @Lob
    @Column(name = "tool_payload_json")
    private String toolPayloadJson;

}
