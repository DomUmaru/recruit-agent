package com.recruit.agent.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recruit.agent.chat.dto.ChatRequest;
import com.recruit.agent.chat.dto.ChatResponse;
import com.recruit.agent.chat.dto.ChatStreamEvent;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.chat.service.ChatApplicationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatApplicationService chatApplicationService;

    @Test
    void shouldReturnChatResponse() throws Exception {
        ChatResponse response = new ChatResponse();
        response.setSessionNo("session-1");
        response.setScene(ChatScene.SEARCH);
        response.setToolName("searchCandidateByJDTool");
        response.setSummary("已完成候选人搜索，返回 1 位候选人。");

        when(chatApplicationService.chat(any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sessionNo": "session-1",
                      "userId": "user-1",
                      "message": "找 Java 候选人"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionNo").value("session-1"))
            .andExpect(jsonPath("$.scene").value("SEARCH"))
            .andExpect(jsonPath("$.toolName").value("searchCandidateByJDTool"));
    }

    @Test
    void shouldStartSseStream() throws Exception {
        ChatStreamEvent startEvent = new ChatStreamEvent();
        startEvent.setEvent("start");
        startEvent.setData("ok");

        ChatStreamEvent tokenEvent = new ChatStreamEvent();
        tokenEvent.setEvent("token");
        tokenEvent.setData("片段");

        when(chatApplicationService.stream(any(ChatRequest.class))).thenReturn(List.of(startEvent, tokenEvent));

        mockMvc.perform(post("/api/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "message": "找 Java 候选人"
                    }
                    """))
            .andExpect(request().asyncStarted())
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.allOf(
                org.hamcrest.Matchers.containsString("event:start"),
                org.hamcrest.Matchers.containsString("event:token")
            )));
    }

    @Test
    void shouldEmitStructuredErrorEventWhenStreamFails() throws Exception {
        doThrow(new IllegalStateException("stream failed"))
            .when(chatApplicationService)
            .stream(any(ChatRequest.class));

        mockMvc.perform(post("/api/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "message": "找 Java 候选人"
                    }
                    """))
            .andExpect(request().asyncStarted())
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.allOf(
                org.hamcrest.Matchers.containsString("event:error"),
                org.hamcrest.Matchers.containsString("CHAT_STREAM_ERROR"),
                org.hamcrest.Matchers.containsString("stream failed")
            )));
    }
}
