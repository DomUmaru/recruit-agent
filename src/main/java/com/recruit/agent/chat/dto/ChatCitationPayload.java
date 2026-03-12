package com.recruit.agent.chat.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * citation 事件载荷。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatCitationPayload {

    private String candidateId;

    private String candidateNo;

    private String candidateName;

    private List<ChatCitationSnippet> snippets;

    /**
     * citation 片段。
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ChatCitationSnippet {

        private String chunkId;

        private String docId;

        private String section;

        private Integer page;

        private String content;
    }
}
