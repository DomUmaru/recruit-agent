package com.recruit.agent.agent.selection;

import com.recruit.agent.chat.state.ChatSessionState;
import java.util.List;

/**
 * 候选人选择解析服务。
 */
public interface CandidateSelectionService {

    /**
     * 从自然语言中解析当前选择的候选人范围。
     *
     * @param state 当前会话状态
     * @param userInput 用户输入
     * @return 解析出的候选人 ID 列表；如果无法解析则返回 null
     */
    List<String> resolveSelectedCandidateIds(ChatSessionState state, String userInput);
}
