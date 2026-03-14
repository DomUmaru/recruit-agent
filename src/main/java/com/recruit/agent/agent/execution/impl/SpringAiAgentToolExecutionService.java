package com.recruit.agent.agent.execution.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.agent.execution.AgentToolExecutionResult;
import com.recruit.agent.agent.execution.AgentToolExecutionService;
import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.agent.tool.springai.CompareCandidatesTools;
import com.recruit.agent.agent.tool.springai.InterviewQuestionTools;
import com.recruit.agent.agent.tool.springai.RefineSearchTools;
import com.recruit.agent.agent.tool.springai.SearchCandidateTools;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.chat.state.ChatSessionState;
import com.recruit.agent.comparison.vo.CandidateComparisonResponse;
import com.recruit.agent.interview.vo.InterviewQuestionResponse;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
@ConditionalOnBean(ChatClient.class)
public class SpringAiAgentToolExecutionService implements AgentToolExecutionService {

    private static final Logger log = LoggerFactory.getLogger(SpringAiAgentToolExecutionService.class);

    private final ChatClient recruitAgentChatClient;
    private final DeterministicAgentToolExecutionService deterministicAgentToolExecutionService;
    private final CompareCandidatesTools compareCandidatesTools;
    private final InterviewQuestionTools interviewQuestionTools;
    private final SearchCandidateTools searchCandidateTools;
    private final RefineSearchTools refineSearchTools;
    private final ObjectMapper objectMapper;

    public SpringAiAgentToolExecutionService(@Qualifier("recruitAgentChatClient") ChatClient recruitAgentChatClient,
                                             DeterministicAgentToolExecutionService deterministicAgentToolExecutionService,
                                             CompareCandidatesTools compareCandidatesTools,
                                             InterviewQuestionTools interviewQuestionTools,
                                             SearchCandidateTools searchCandidateTools,
                                             RefineSearchTools refineSearchTools,
                                             ObjectMapper objectMapper) {
        this.recruitAgentChatClient = recruitAgentChatClient;
        this.deterministicAgentToolExecutionService = deterministicAgentToolExecutionService;
        this.compareCandidatesTools = compareCandidatesTools;
        this.interviewQuestionTools = interviewQuestionTools;
        this.searchCandidateTools = searchCandidateTools;
        this.refineSearchTools = refineSearchTools;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentToolExecutionResult execute(AgentRouteDecision routeDecision, ChatSessionState state, String userInput) {
        if (routeDecision.getScene() == ChatScene.SEARCH
            || routeDecision.getScene() == ChatScene.COMPARE
            || routeDecision.getScene() == ChatScene.INTERVIEW
            || routeDecision.getScene() == ChatScene.FILTER_REFINE) {
            return deterministicAgentToolExecutionService.execute(routeDecision, state, userInput);
        }
        Object availableTool = resolveTool(routeDecision);
        try {
            String payload = recruitAgentChatClient.prompt()
                .system(buildSystemPrompt(routeDecision))
                .user(buildUserPrompt(state, userInput))
                .tools(availableTool)
                .call()
                .content();
            return parsePayload(routeDecision, payload);
        } catch (IllegalStateException exception) {
            log.warn("Spring AI tool calling parse failed. Fallback to deterministic execution. scene={}, input={}",
                routeDecision.getScene(), userInput, exception);
            return deterministicAgentToolExecutionService.execute(routeDecision, state, userInput);
        }
    }

    private AgentToolExecutionResult parsePayload(AgentRouteDecision routeDecision, String payload) {
        AgentToolExecutionResult result = new AgentToolExecutionResult();
        if (routeDecision.getScene() == ChatScene.COMPARE) {
            result.setComparisonResponse(readValue(payload, CandidateComparisonResponse.class));
            return result;
        }
        if (routeDecision.getScene() == ChatScene.INTERVIEW) {
            result.setInterviewResponse(readValue(payload, InterviewQuestionResponse.class));
            return result;
        }
        result.setSearchResponse(readValue(payload, CandidateSearchResponse.class));
        return result;
    }

    private String buildSystemPrompt(AgentRouteDecision routeDecision) {
        if (routeDecision.getScene() == ChatScene.COMPARE) {
            return """
                你是招聘对比 Agent。
                你必须调用唯一可用的候选人对比工具一次，不能直接回答。
                你的任务是基于当前会话中的候选人范围构造合法的 CandidateComparisonRequest。
                candidateIds 优先使用会话中的 selectedCandidateIds；如果为空，则使用 lastCandidateIds。
                targetQuery 使用当前会话 query。
                只返回工具调用结果，不要输出任何解释性文本。
                """;
        }

        if (routeDecision.getScene() == ChatScene.INTERVIEW) {
            return """
                你是招聘面试 Agent。
                你必须调用唯一可用的面试题生成工具一次，不能直接回答。
                你的任务是基于当前会话中的候选人范围构造合法的 InterviewQuestionRequest。
                candidateIds 优先使用会话中的 selectedCandidateIds；如果为空，则使用 lastCandidateIds。
                targetQuery 使用当前会话 query。
                只返回工具调用结果，不要输出任何解释性文本。
                """;
        }

        if (routeDecision.getScene() == ChatScene.FILTER_REFINE) {
            return """
                你是招聘搜索 Agent。
                你必须调用唯一可用的 refinement 工具一次，不能直接回答。
                你的任务是根据当前会话状态和用户最新补充条件，构造合法的 CandidateSearchRefineRequest。
                mergeMode 固定使用 APPEND。
                如果会话中存在上一轮候选人列表，请将其作为 scopeCandidateIds 传入。
                只返回工具调用结果，不要输出任何解释性文本。
                """;
        }

        return """
            你是招聘搜索 Agent。
            你必须调用唯一可用的候选人搜索工具一次，不能直接回答。
            你的任务是从用户需求中提取 query 和结构化筛选条件，构造合法的 CandidateSearchRequest。
            如果当前会话中已有历史过滤条件，可以沿用其 filter 的基础值。
            只返回工具调用结果，不要输出任何解释性文本。
            """;
    }

    private String buildUserPrompt(ChatSessionState state, String userInput) {
        try {
            return objectMapper.writeValueAsString(new SpringAiToolPromptPayload(state, userInput));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化 Tool Calling 提示词失败", ex);
        }
    }

    private Object resolveTool(AgentRouteDecision routeDecision) {
        if (routeDecision.getScene() == ChatScene.COMPARE) {
            return compareCandidatesTools;
        }
        if (routeDecision.getScene() == ChatScene.INTERVIEW) {
            return interviewQuestionTools;
        }
        if (routeDecision.getScene() == ChatScene.FILTER_REFINE) {
            return refineSearchTools;
        }
        return searchCandidateTools;
    }

    private <T> T readValue(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(extractJsonPayload(payload), type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("解析 Tool Calling 结果失败", ex);
        }
    }

    private String extractJsonPayload(String payload) {
        if (payload == null) {
            throw new IllegalStateException("Tool Calling 返回为空");
        }
        String trimmed = payload.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalStateException("Tool Calling 返回为空");
        }
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            if (firstLineBreak >= 0) {
                trimmed = trimmed.substring(firstLineBreak + 1).trim();
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }

        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1);
        }

        int arrayStart = trimmed.indexOf('[');
        int arrayEnd = trimmed.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return trimmed.substring(arrayStart, arrayEnd + 1);
        }

        return trimmed;
    }

    private record SpringAiToolPromptPayload(ChatSessionState state, String userInput) {
    }
}
