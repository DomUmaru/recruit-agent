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
 * Persistent chat session entity.
 */
@Entity
@Table(name = "chat_session")
@Getter
@Setter
@NoArgsConstructor
public class ChatSession extends BaseAuditEntity {

    /**
     * Bound position ID for the current session.
     */
    @Column(name = "position_id", length = 64)
    private String positionId;

    /**
     * Session identifier.
     */
    @Column(name = "session_no", nullable = false, unique = true, length = 64)
    private String sessionNo;

    /**
     * Session owner identifier.
     */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    /**
     * Current scene.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_scene", nullable = false, length = 32)
    private ChatScene currentScene;

    /**
     * Session status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ChatSessionStatus status;

    /**
     * Current accumulated query text.
     */
    @Column(name = "current_query", length = 1000)
    private String currentQuery;

    /**
     * Current accumulated filter JSON.
     */
    @Lob
    @Column(name = "filters_json")
    private String filtersJson;

    /**
     * Previous-turn candidate IDs as JSON.
     */
    @Lob
    @Column(name = "last_candidate_ids_json")
    private String lastCandidateIdsJson;

    /**
     * Selected candidate IDs as JSON.
     */
    @Lob
    @Column(name = "selected_candidate_ids_json")
    private String selectedCandidateIdsJson;

    /**
     * Current sort mode.
     */
    @Column(name = "sort_mode", length = 64)
    private String sortMode;

    /**
     * Full session state snapshot as JSON.
     */
    @Lob
    @Column(name = "session_state_json")
    private String sessionStateJson;

    /**
     * Persistent messages in this session.
     */
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();
}
