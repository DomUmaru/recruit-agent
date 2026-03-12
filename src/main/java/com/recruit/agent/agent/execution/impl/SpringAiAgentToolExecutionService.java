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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 基于 Spring AI Tool Calling 的执行器。
 * 与 deterministic 版本的区别是：
 * - deterministic：代码直接组装 request
 * - Spring AI：把 state + userInput 交给模型，由模型构造 tool request，再调用受限 tool
 */
@Primary
@Service
@ConditionalOnBean(ChatClient.class)
public class SpringAiAgentToolExecutionService implements AgentToolExecutionService {

    private final ChatClient recruitAgentChatClient;
    private final CompareCandidatesTools compareCandidatesTools;
    private final InterviewQuestionTools interviewQuestionTools;
    private final SearchCandidateTools searchCandidateTools;
    private final RefineSearchTools refineSearchTools;
    private final ObjectMapper objectMapper;

    public SpringAiAgentToolExecutionService(@Qualifier("recruitAgentChatClient") ChatClient recruitAgentChatClient,
                                             CompareCandidatesTools compareCandidatesTools,
                                             InterviewQuestionTools interviewQuestionTools,
                                             SearchCandidateTools searchCandidateTools,
                                             RefineSearchTools refineSearchTools,
                                             ObjectMapper objectMapper) {
        this.recruitAgentChatClient = recruitAgentChatClient;
        this.compareCandidatesTools = compareCandidatesTools;
        this.interviewQuestionTools = interviewQuestionTools;
        this.searchCandidateTools = searchCandidateTools;
        this.refineSearchTools = refineSearchTools;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentToolExecutionResult execute(AgentRouteDecision routeDecision, ChatSessionState state, String userInput) {
        // 根据 router 的场景只暴露一个可用 tool，避免模型越权调用不相关能力。
        Object availableTool = resolveTool(routeDecision);
        AgentToolExecutionResult result = new AgentToolExecutionResult();

        // system prompt 约束“必须调 tool，不能直接回答”；user prompt 携带当前 state 和用户输入。
        String payload = recruitAgentChatClient.prompt()
            .system(buildSystemPrompt(routeDecision))
            .user(buildUserPrompt(state, userInput))
            .tools(availableTool)
            .call()
            .content();

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
                """;
        }

        if (routeDecision.getScene() == ChatScene.INTERVIEW) {
            return """
                你是招聘面试 Agent。
                你必须调用唯一可用的面试题生成工具一次，不能直接回答。
                你的任务是基于当前会话中的候选人范围构造合法的 InterviewQuestionRequest。
                candidateIds 优先使用会话中的 selectedCandidateIds；如果为空，则使用 lastCandidateIds。
                targetQuery 使用当前会话 query。
                """;
        }

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
            如果当前会话中已有历史过滤条件，可以沿用其 filter 的基础值。
            """;
    }

    private String buildUserPrompt(ChatSessionState state, String userInput) {
        try {
            // 统一把上下文序列化成结构化 JSON，减少模型从自然语言上下文中猜测状态的概率。
            return objectMapper.writeValueAsString(new SpringAiToolPromptPayload(state, userInput));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化 Tool Calling 提示词失败", ex);
        }
    }

    private Object resolveTool(AgentRouteDecision routeDecision) {
        // 当前场景只开放一个 tool，是这条链路里最重要的 guardrail 之一。
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
            // Tool Calling 返回的是 JSON 字符串，这里再反序列化成内部 VO。
            return objectMapper.readValue(payload, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("解析 Tool Calling 结果失败", ex);
        }
    }

    private record SpringAiToolPromptPayload(ChatSessionState state, String userInput) {
    }
}
