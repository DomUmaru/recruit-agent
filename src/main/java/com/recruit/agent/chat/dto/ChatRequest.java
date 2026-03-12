package com.recruit.agent.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 统一聊天请求。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatRequest {

    /**
     * 会话编号；为空时自动创建。
     */
    private String sessionNo;

    /**
     * 用户编号；为空时使用匿名用户。
     */
    private String userId;

    /**
     * 用户输入。
     */
    @NotBlank
    private String message;
}
