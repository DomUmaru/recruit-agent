package com.recruit.agent.agent.router.impl;

import com.recruit.agent.agent.router.AgentRouterService;
import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.agent.router.dto.AgentRoutingContext;
import com.recruit.agent.chat.model.ChatScene;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Agent 路由器。
 * 作用是把用户输入粗分到具体业务场景，而不是直接做搜索或对比。
 * 当前是规则式路由：根据关键词和会话历史在 SEARCH / FILTER_REFINE / COMPARE / INTERVIEW 中选择。
 */
@Service
public class AgentRouterServiceImpl implements AgentRouterService {

    private static final String SEARCH_TOOL_NAME = "searchCandidateByJDTool";
    private static final String REFINE_TOOL_NAME = "refineSearchFilterTool";
    private static final String COMPARE_TOOL_NAME = "compareCandidatesTool";
    private static final String INTERVIEW_TOOL_NAME = "generateInterviewQuestionsTool";

    private static final List<String> REFINE_KEYWORDS = List.of(
        "只要", "再加", "追加", "筛选", "过滤", "排除", "限定", "仅看", "优先", "不要", "只看", "保留"
    );

    private static final List<String> COMPARE_KEYWORDS = List.of(
        "对比", "比较", "比一下", "比一比", "横向看", "pk"
    );

    private static final List<String> INTERVIEW_KEYWORDS = List.of(
        "面试", "面试题", "题目", "提问", "追问", "八股", "interview"
    );

    @Override
    public AgentRouteDecision route(AgentRoutingContext context) {
        AgentRoutingContext safeContext = context == null ? new AgentRoutingContext() : context;
        String userInput = normalize(safeContext.getUserInput());
        boolean hasHistory = hasHistory(safeContext);
        boolean refineIntent = hasHistory && isRefineIntent(userInput);
        boolean compareIntent = hasHistory && isCompareIntent(userInput);
        boolean interviewIntent = hasHistory && isInterviewIntent(userInput);

        AgentRouteDecision decision = new AgentRouteDecision();

        // 面试题和对比都依赖上轮候选人范围，因此要求存在历史状态。
        if (interviewIntent) {
            decision.setScene(ChatScene.INTERVIEW);
            decision.setToolName(INTERVIEW_TOOL_NAME);
            decision.setHistoryRequired(true);
            decision.setReason("检测到面试题生成语义，且会话中存在可复用的候选人范围");
            return decision;
        }

        if (compareIntent) {
            decision.setScene(ChatScene.COMPARE);
            decision.setToolName(COMPARE_TOOL_NAME);
            decision.setHistoryRequired(true);
            decision.setReason("检测到候选人对比语义，且会话中存在可复用的候选人列表");
            return decision;
        }

        if (refineIntent) {
            decision.setScene(ChatScene.FILTER_REFINE);
            decision.setToolName(REFINE_TOOL_NAME);
            decision.setHistoryRequired(true);
            decision.setReason("检测到追加或覆盖筛选语义，且会话中存在可复用的搜索历史");
            return decision;
        }

        // 默认兜底到 SEARCH，表示把当前输入当成一轮新的招聘搜索需求。
        decision.setScene(ChatScene.SEARCH);
        decision.setToolName(SEARCH_TOOL_NAME);
        decision.setHistoryRequired(false);
        decision.setReason(hasHistory
            ? "当前输入更像新的搜索需求，优先按搜索场景处理"
            : "未检测到可复用历史状态，按新搜索处理");
        return decision;
    }

    private boolean hasHistory(AgentRoutingContext context) {
        // 这里的“历史”不是完整聊天记录，而是当前任务链继续执行所需的最小上下文。
        return context.getCurrentScene() != null
            || hasText(context.getCurrentQuery())
            || hasText(context.getFiltersJson())
            || hasText(context.getLastCandidateIdsJson());
    }

    private boolean isRefineIntent(String userInput) {
        if (userInput.isBlank()) {
            return false;
        }
        return REFINE_KEYWORDS.stream().anyMatch(userInput::contains);
    }

    private boolean isCompareIntent(String userInput) {
        if (userInput.isBlank()) {
            return false;
        }
        return COMPARE_KEYWORDS.stream().anyMatch(userInput::contains);
    }

    private boolean isInterviewIntent(String userInput) {
        if (userInput.isBlank()) {
            return false;
        }
        return INTERVIEW_KEYWORDS.stream().anyMatch(userInput::contains);
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}
