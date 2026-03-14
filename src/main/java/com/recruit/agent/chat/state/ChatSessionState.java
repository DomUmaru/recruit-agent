package com.recruit.agent.chat.state;

import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * In-memory chat session state.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatSessionState {

    /**
     * Bound position ID for the current chat session.
     */
    private String positionId;

    /**
     * Current scene.
     */
    private ChatScene currentScene;

    /**
     * Current accumulated query.
     */
    private String currentQuery;

    /**
     * Current accumulated filter.
     */
    private CandidateSearchFilter filter = new CandidateSearchFilter();

    /**
     * Candidate IDs returned in the previous turn.
     */
    private List<String> lastCandidateIds;

    /**
     * Candidate IDs selected from the previous turn.
     */
    private List<String> selectedCandidateIds;

    /**
     * Current sort mode.
     */
    private String sortMode;
}
