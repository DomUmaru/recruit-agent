package com.recruit.agent.chat.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * compare 场景稳定输出载荷。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatComparisonPayload {

    private String targetQuery;

    private String summary;

    private List<ChatComparisonCandidatePayload> candidates;
}
