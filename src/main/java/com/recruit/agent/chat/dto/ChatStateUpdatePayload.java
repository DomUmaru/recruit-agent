package com.recruit.agent.chat.dto;

import com.recruit.agent.chat.model.ChatScene;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * state_update 事件载荷。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatStateUpdatePayload {

    private ChatScene scene;

    private String currentQuery;

    private List<String> lastCandidateIds;
}
