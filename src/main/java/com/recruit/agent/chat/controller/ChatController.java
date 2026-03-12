package com.recruit.agent.chat.controller;

import com.recruit.agent.chat.dto.ChatErrorPayload;
import com.recruit.agent.chat.dto.ChatRequest;
import com.recruit.agent.chat.dto.ChatResponse;
import com.recruit.agent.chat.dto.ChatStreamEvent;
import com.recruit.agent.chat.service.ChatApplicationService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 聊天接口控制器。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatApplicationService chatApplicationService;

    public ChatController(ChatApplicationService chatApplicationService) {
        this.chatApplicationService = chatApplicationService;
    }

    /**
     * 普通聊天接口。
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatApplicationService.chat(request);
    }

    /**
     * SSE 流式聊天接口。
     *
     * @param request 聊天请求
     * @return SSE 发射器
     */
    @PostMapping(value = "/stream", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            List<ChatStreamEvent> events = chatApplicationService.stream(request);
            for (ChatStreamEvent event : events) {
                emitter.send(SseEmitter.event().name(event.getEvent()).data(event.getData()));
            }
            emitter.complete();
        } catch (IOException ex) {
            sendErrorEvent(emitter, buildErrorPayload("SSE_WRITE_ERROR", "SSE 事件发送失败", true));
            emitter.completeWithError(ex);
        } catch (Exception ex) {
            sendErrorEvent(emitter, buildErrorPayload("CHAT_STREAM_ERROR", ex.getMessage(), false));
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    private void sendErrorEvent(SseEmitter emitter, ChatErrorPayload payload) {
        try {
            emitter.send(SseEmitter.event().name("error").data(payload));
        } catch (IOException ignored) {
            // Ignore secondary write failure when completing the stream.
        }
    }

    private ChatErrorPayload buildErrorPayload(String code, String message, boolean retryable) {
        ChatErrorPayload payload = new ChatErrorPayload();
        payload.setCode(code);
        payload.setMessage(message == null || message.isBlank() ? "未知错误" : message);
        payload.setRetryable(retryable);
        return payload;
    }
}
