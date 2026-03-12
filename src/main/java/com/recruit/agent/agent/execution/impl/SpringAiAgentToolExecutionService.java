package com.recruit.agent.agent.execution.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.agent.execution.AgentToolExecutionService;
import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.agent.tool.springai.RefineSearchTools;
import com.recruit.agent.agent.tool.springai.SearchCandidateTools;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.chat.state.ChatSessionState;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 基于 Spring AI Tool Calling 的工具执行服务。
 */
@Primary
@Service
@ConditionalOnBean(ChatClient.class)
public class SpringAiAgentToolExecutionService implements AgentToolExecutionService {

    private final ChatClient recruitAgentChatClient;
    private final SearchCandidateTools searchCandidateTools;
    private final RefineSearchTools refineSearchTools;
    private final ObjectMapper objectMapper;

    public SpringAiAgentToolExecutionService(@Qualifier("recruitAgentChatClient") ChatClient recruitAgentChatClient,
                                             SearchCandidateTools searchCandidateTools,
                                             RefineSearchTools refineSearchTools,
                                             ObjectMapper objectMapper) {
        this.recruitAgentChatClient = recruitAgentChatClient;
        this.searchCandidateTools = searchCandidateTools;
        this.refineSearchTools = refineSearchTools;
        this.objectMapper = objectMapper;
    }

    @Override
    public CandidateSearchResponse execute(AgentRouteDecision routeDecision, ChatSessionState state, String userInput) {
        Object availableTool = routeDecision.getScene() == ChatScene.FILTER_REFINE
            ? refineSearchTools
            : searchCandidateTools;

        return recruitAgentChatClient.prompt()
            .system(buildSystemPrompt(routeDecision))
            .user(buildUserPrompt(state, userInput))
            .tools(availableTool)
            .call()
            .entity(CandidateSearchResponse.class);
    }

    private String buildSystemPrompt(AgentRouteDecision routeDecision) {
        if (routeDecision.getScene() == ChatScene.FILTER_REFINE) {
            return """
                你是招聘搜索 Agent。
                你必须调用唯一可用的 refinement 工具一次，不能直接回答。
                你的任务是根据当前会话状态和用户最新补充条件，构造合法的 CandidateSearchRefineRequest。
                mergeMode 固定使用 APPEND。
                如果会话中存在上一轮候选人列表，请将其作为 scopeCandidateIds 传入。
                """;
        }

        return """
            你是招聘搜索 Agent。
            你必须调用唯一可用的候选人搜索工具一次，不能直接回答。
            你的任务是从用户需求中提取 query 和结构化筛选条件，构造合法的 CandidateSearchRequest。
            如果当前会话中已有历史过滤条件，可以沿用为 filter 的基础值。
            """;
    }

    private String buildUserPrompt(ChatSessionState state, String userInput) {
        try {
            return objectMapper.writeValueAsString(new SpringAiToolPromptPayload(state, userInput));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化 Tool Calling 提示词失败", ex);
        }
    }

    private record SpringAiToolPromptPayload(ChatSessionState state, String userInput) {
    }
}
