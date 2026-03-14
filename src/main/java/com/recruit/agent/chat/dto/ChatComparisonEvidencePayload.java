package com.recruit.agent.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * compare 场景证据载荷。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatComparisonEvidencePayload {

    private String section;

    private Integer page;

    private String content;
}
