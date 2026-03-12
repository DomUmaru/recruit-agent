package com.recruit.agent.agent.router.impl;

import com.recruit.agent.agent.router.AgentRouterService;
import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.agent.router.dto.AgentRoutingContext;
import com.recruit.agent.chat.model.ChatScene;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Agent 路由服务实现。
 */
@Service
public class AgentRouterServiceImpl implements AgentRouterService {

    private static final String SEARCH_TOOL_NAME = "searchCandidateByJDTool";
    private static final String REFINE_TOOL_NAME = "refineSearchFilterTool";

    private static final List<String> REFINE_KEYWORDS = List.of(
        "只要", "再加", "追加", "筛选", "过滤", "排除", "限定", "仅看", "优先", "不要", "只看", "保留"
    );

    @Override
    public AgentRouteDecision route(AgentRoutingContext context) {
        AgentRoutingContext safeContext = context == null ? new AgentRoutingContext() : context;
        String userInput = normalize(safeContext.getUserInput());
        boolean hasHistory = hasHistory(safeContext);
        boolean refineIntent = hasHistory && isRefineIntent(userInput);

        AgentRouteDecision decision = new AgentRouteDecision();
        if (refineIntent) {
            decision.setScene(ChatScene.FILTER_REFINE);
            decision.setToolName(REFINE_TOOL_NAME);
            decision.setHistoryRequired(true);
            decision.setReason("检测到追加/覆盖筛选语义，且会话中存在可复用的搜索历史");
            return decision;
        }

        decision.setScene(ChatScene.SEARCH);
        decision.setToolName(SEARCH_TOOL_NAME);
        decision.setHistoryRequired(false);
        decision.setReason(hasHistory
            ? "当前输入更像新的搜索需求，优先按搜索场景处理"
            : "未检测到可复用历史状态，按新搜索处理");
        return decision;
    }

    private boolean hasHistory(AgentRoutingContext context) {
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

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}
