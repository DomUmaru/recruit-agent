package com.recruit.agent.agent.router.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.agent.router.AgentRouterService;
import com.recruit.agent.agent.router.dto.AgentRouteDecision;
import com.recruit.agent.agent.router.dto.AgentRoutingContext;
import com.recruit.agent.chat.model.ChatScene;
import com.recruit.agent.llm.LlmGenerationService;
import com.recruit.agent.llm.UnavailableLlmGenerationService;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgentRouterServiceImpl implements AgentRouterService {

    private static final Logger log = LoggerFactory.getLogger(AgentRouterServiceImpl.class);

    private static final String SEARCH_TOOL_NAME = "searchCandidateByJDTool";
    private static final String REFINE_TOOL_NAME = "refineSearchFilterTool";
    private static final String COMPARE_TOOL_NAME = "compareCandidatesTool";
    private static final String INTERVIEW_TOOL_NAME = "generateInterviewQuestionsTool";

    private static final List<String> REFINE_KEYWORDS = List.of(
        "\u53ea\u8981",
        "\u518d\u52a0",
        "\u8ffd\u52a0",
        "\u7b5b\u9009",
        "\u8fc7\u6ee4",
        "\u6392\u9664",
        "\u9650\u5b9a",
        "\u4ec5\u770b",
        "\u4f18\u5148",
        "\u4e0d\u8981",
        "\u53ea\u770b",
        "\u4fdd\u7559",
        "\u7ee7\u7eed",
        "\u91cd\u65b0",
        "\u91cd\u65b0\u7b5b\u9009",
        "\u91cd\u65b0\u6765",
        "\u91cd\u7b5b",
        "\u6362\u4e00\u6279",
        "\u8fd9\u6b21\u6309"
    );

    private static final List<String> COMPARE_KEYWORDS = List.of(
        "\u5bf9\u6bd4",
        "\u6bd4\u8f83",
        "\u6bd4\u4e00\u4e2a",
        "\u6bd4\u4e00\u6bd4",
        "\u6a2a\u5411\u770b",
        "pk"
    );

    private static final List<String> INTERVIEW_KEYWORDS = List.of(
        "\u9762\u8bd5",
        "\u9762\u8bd5\u9898",
        "\u9898\u76ee",
        "\u63d0\u95ee",
        "\u8ffd\u95ee",
        "\u516b\u80a1",
        "interview"
    );

    private final LlmGenerationService llmGenerationService;
    private final ObjectMapper objectMapper;

    public AgentRouterServiceImpl() {
        this(new UnavailableLlmGenerationService(), new ObjectMapper());
    }

    public AgentRouterServiceImpl(LlmGenerationService llmGenerationService, ObjectMapper objectMapper) {
        this.llmGenerationService = llmGenerationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentRouteDecision route(AgentRoutingContext context) {
        AgentRoutingContext safeContext = context == null ? new AgentRoutingContext() : context;

        AgentRouteDecision ruleDecision = routeWithRules(safeContext);
        if (ruleDecision.getScene() == ChatScene.FILTER_REFINE
            || ruleDecision.getScene() == ChatScene.COMPARE
            || ruleDecision.getScene() == ChatScene.INTERVIEW) {
            log.info("Agent router final scene={} via rules-priority. userInput='{}', currentQuery='{}'.",
                ruleDecision.getScene(), safeValue(safeContext.getUserInput()), safeValue(safeContext.getCurrentQuery()));
            return ruleDecision;
        }

        AgentRouteDecision llmDecision = routeWithLlm(safeContext);
        if (llmDecision != null) {
            log.info("Agent router final scene={} via llm. userInput='{}', currentQuery='{}'.",
                llmDecision.getScene(), safeValue(safeContext.getUserInput()), safeValue(safeContext.getCurrentQuery()));
            return llmDecision;
        }

        log.info("Agent router final scene={} via rules. userInput='{}', currentQuery='{}'.",
            ruleDecision.getScene(), safeValue(safeContext.getUserInput()), safeValue(safeContext.getCurrentQuery()));
        return ruleDecision;
    }

    private AgentRouteDecision routeWithLlm(AgentRoutingContext context) {
        if (!llmGenerationService.isAvailable() || !hasText(context.getUserInput())) {
            return null;
        }

        try {
            String response = llmGenerationService.generate(buildRouterSystemPrompt(), buildRouterUserPrompt(context));
            if (!hasText(response)) {
                return null;
            }

            LlmRouteOutput output = objectMapper.readValue(response, LlmRouteOutput.class);
            ChatScene scene = parseScene(output.getScene());
            if (scene == null) {
                return null;
            }

            boolean historyRequired = requiresHistory(scene);
            if (historyRequired && !hasHistory(context)) {
                log.info("LLM router suggested scene={} but no reusable history exists. Falling back to rules.", scene);
                return null;
            }

            AgentRouteDecision decision = new AgentRouteDecision();
            decision.setScene(scene);
            decision.setToolName(resolveToolName(scene));
            decision.setHistoryRequired(historyRequired);
            decision.setReason(hasText(output.getReason()) ? "llm-router: " + output.getReason().trim() : "llm-router");
            return decision;
        } catch (Exception exception) {
            log.warn("LLM router failed. Falling back to rule-based routing.", exception);
            return null;
        }
    }

    private AgentRouteDecision routeWithRules(AgentRoutingContext context) {
        String userInput = normalize(context.getUserInput());
        boolean hasHistory = hasHistory(context);
        boolean refineIntent = hasHistory && isRefineIntent(userInput);
        boolean compareIntent = hasHistory && isCompareIntent(userInput);
        boolean interviewIntent = hasHistory && isInterviewIntent(userInput);

        AgentRouteDecision decision = new AgentRouteDecision();
        if (interviewIntent) {
            decision.setScene(ChatScene.INTERVIEW);
            decision.setToolName(INTERVIEW_TOOL_NAME);
            decision.setHistoryRequired(true);
            decision.setReason("rule-interview");
            return decision;
        }

        if (compareIntent) {
            decision.setScene(ChatScene.COMPARE);
            decision.setToolName(COMPARE_TOOL_NAME);
            decision.setHistoryRequired(true);
            decision.setReason("rule-compare");
            return decision;
        }

        if (refineIntent) {
            decision.setScene(ChatScene.FILTER_REFINE);
            decision.setToolName(REFINE_TOOL_NAME);
            decision.setHistoryRequired(true);
            decision.setReason("rule-refine");
            return decision;
        }

        decision.setScene(ChatScene.SEARCH);
        decision.setToolName(SEARCH_TOOL_NAME);
        decision.setHistoryRequired(false);
        decision.setReason(hasHistory ? "rule-new-search-with-history" : "rule-new-search");
        return decision;
    }

    private boolean requiresHistory(ChatScene scene) {
        return scene == ChatScene.FILTER_REFINE || scene == ChatScene.COMPARE || scene == ChatScene.INTERVIEW;
    }

    private String buildRouterSystemPrompt() {
        return """
            You are an intent router for a recruitment agent.
            Classify the user message into exactly one scene:
            - SEARCH: a new candidate search request
            - FILTER_REFINE: narrowing or updating the previous search result set
            - COMPARE: comparing candidates from prior results
            - INTERVIEW: generating interview questions for candidates from prior results

            Return JSON only:
            {"scene":"SEARCH|FILTER_REFINE|COMPARE|INTERVIEW","reason":"short reason"}

            Constraints:
            - Do not invent candidates, filters, or tool parameters.
            - If the user explicitly asks for comparison, return COMPARE.
            - If the user explicitly asks for interview questions, return INTERVIEW.
            - If the user narrows previous candidates, return FILTER_REFINE.
            - Otherwise return SEARCH.
            """;
    }

    private String buildRouterUserPrompt(AgentRoutingContext context) {
        return """
            User input:
            %s

            Current scene:
            %s

            Current query:
            %s

            Filters json:
            %s

            Last candidate ids json:
            %s
            """.formatted(
            safeValue(context.getUserInput()),
            context.getCurrentScene() == null ? "" : context.getCurrentScene().name(),
            safeValue(context.getCurrentQuery()),
            safeValue(context.getFiltersJson()),
            safeValue(context.getLastCandidateIdsJson())
        );
    }

    private ChatScene parseScene(String scene) {
        if (!hasText(scene)) {
            return null;
        }
        try {
            return ChatScene.valueOf(scene.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String resolveToolName(ChatScene scene) {
        return switch (scene) {
            case FILTER_REFINE -> REFINE_TOOL_NAME;
            case COMPARE -> COMPARE_TOOL_NAME;
            case INTERVIEW -> INTERVIEW_TOOL_NAME;
            case SEARCH -> SEARCH_TOOL_NAME;
            default -> SEARCH_TOOL_NAME;
        };
    }

    private boolean hasHistory(AgentRoutingContext context) {
        return context.getCurrentScene() != null
            || hasText(context.getCurrentQuery())
            || hasText(context.getFiltersJson())
            || hasText(context.getLastCandidateIdsJson());
    }

    private boolean isRefineIntent(String userInput) {
        return hasText(userInput) && REFINE_KEYWORDS.stream().anyMatch(userInput::contains);
    }

    private boolean isCompareIntent(String userInput) {
        return hasText(userInput) && COMPARE_KEYWORDS.stream().anyMatch(userInput::contains);
    }

    private boolean isInterviewIntent(String userInput) {
        return hasText(userInput) && INTERVIEW_KEYWORDS.stream().anyMatch(userInput::contains);
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private String safeValue(String text) {
        return text == null ? "" : text;
    }

    private static class LlmRouteOutput {
        private String scene;
        private String reason;

        public String getScene() {
            return scene;
        }

        public void setScene(String scene) {
            this.scene = scene;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
