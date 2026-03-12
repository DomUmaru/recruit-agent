package com.recruit.agent.chat.state;

import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 聊天会话运行态对象。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatSessionState {

    /**
     * 当前场景。
     */
    private ChatScene currentScene;

    /**
     * 当前查询语句。
     */
    private String currentQuery;

    /**
     * 当前过滤条件。
     */
    private CandidateSearchFilter filter = new CandidateSearchFilter();

    /**
     * 上一轮候选人结果 ID。
     */
    private List<String> lastCandidateIds;

    /**
     * 当前选中候选人 ID。
     */
    private List<String> selectedCandidateIds;

    /**
     * 当前排序方式。
     */
    private String sortMode;
}
