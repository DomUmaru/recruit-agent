package com.recruit.agent.chat.model;

import com.recruit.agent.common.model.BaseAuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 聊天会话实体，用于保存多轮招聘任务的上下文状态。
 */
@Entity
@Table(name = "chat_session")
@Getter
@Setter
@NoArgsConstructor
public class ChatSession extends BaseAuditEntity {

    /**
     * 会话编号。
     */
    @Column(name = "session_no", nullable = false, unique = true, length = 64)
    private String sessionNo;

    /**
     * 发起会话的用户 ID。
     */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    /**
     * 当前会话场景。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_scene", nullable = false, length = 32)
    private ChatScene currentScene;

    /**
     * 会话状态。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ChatSessionStatus status;

    /**
     * 当前轮主查询语句。
     */
    @Column(name = "current_query", length = 1000)
    private String currentQuery;

    /**
     * 当前筛选条件，建议保存为 JSON。
     */
    @Lob
    @Column(name = "filters_json")
    private String filtersJson;

    /**
     * 上一轮候选人结果集 ID 列表，建议保存为 JSON。
     */
    @Lob
    @Column(name = "last_candidate_ids_json")
    private String lastCandidateIdsJson;

    /**
     * 当前选中候选人 ID 列表，建议保存为 JSON。
     */
    @Lob
    @Column(name = "selected_candidate_ids_json")
    private String selectedCandidateIdsJson;

    /**
     * 当前排序方式。
     */
    @Column(name = "sort_mode", length = 64)
    private String sortMode;

    /**
     * 完整会话状态快照，建议保存为 JSON。
     */
    @Lob
    @Column(name = "session_state_json")
    private String sessionStateJson;

    /**
     * 会话下的消息列表。
     */
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

}
