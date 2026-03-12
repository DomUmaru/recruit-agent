package com.recruit.agent.agent.selection.impl;

import com.recruit.agent.agent.selection.CandidateSelectionService;
import com.recruit.agent.chat.state.ChatSessionState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * 基于规则的候选人选择解析服务。
 */
@Service
public class CandidateSelectionServiceImpl implements CandidateSelectionService {

    @Override
    public List<String> resolveSelectedCandidateIds(ChatSessionState state, String userInput) {
        if (state == null || userInput == null || userInput.isBlank()) {
            return null;
        }

        List<String> lastCandidateIds = state.getLastCandidateIds();
        if (lastCandidateIds == null || lastCandidateIds.isEmpty()) {
            return null;
        }

        String normalized = normalize(userInput);

        if (containsAny(normalized, "全部", "所有人", "所有候选人", "全部候选人", "all")) {
            return lastCandidateIds;
        }

        Integer ordinal = extractOrdinal(normalized);
        if (ordinal != null) {
            return pickRange(lastCandidateIds, ordinal - 1, ordinal);
        }

        Integer topN = extractTopN(normalized);
        if (topN != null) {
            return pickRange(lastCandidateIds, 0, topN);
        }

        if (containsAny(normalized, "这两个人", "这两个候选人", "这两位", "这俩")) {
            if (state.getSelectedCandidateIds() != null && state.getSelectedCandidateIds().size() >= 2) {
                return state.getSelectedCandidateIds().subList(0, 2);
            }
            return pickRange(lastCandidateIds, 0, 2);
        }

        if (containsAny(normalized, "这个人", "这位", "这个候选人")) {
            if (state.getSelectedCandidateIds() != null && !state.getSelectedCandidateIds().isEmpty()) {
                return List.of(state.getSelectedCandidateIds().get(0));
            }
            return pickRange(lastCandidateIds, 0, 1);
        }

        return null;
    }

    private Integer extractOrdinal(String text) {
        for (int i = 1; i <= 10; i++) {
            if (text.contains("第" + i + "个") || text.contains("第" + chineseNumber(i) + "个")
                || text.contains("第" + i + "位") || text.contains("第" + chineseNumber(i) + "位")
                || text.contains("第" + colloquialChineseNumber(i) + "个")
                || text.contains("第" + colloquialChineseNumber(i) + "位")) {
                return i;
            }
        }
        return null;
    }

    private Integer extractTopN(String text) {
        for (int i = 1; i <= 10; i++) {
            if (text.contains("前" + i + "个") || text.contains("前" + chineseNumber(i) + "个")
                || text.contains("前" + i + "位") || text.contains("前" + chineseNumber(i) + "位")
                || text.contains("前" + colloquialChineseNumber(i) + "个")
                || text.contains("前" + colloquialChineseNumber(i) + "位")) {
                return i;
            }
        }
        if (containsAny(text, "前俩", "前两位")) {
            return 2;
        }
        return null;
    }

    private List<String> pickRange(List<String> source, int startInclusive, int endExclusive) {
        if (source == null || source.isEmpty() || startInclusive >= source.size()) {
            return List.of();
        }
        int safeStart = Math.max(startInclusive, 0);
        int safeEnd = Math.min(endExclusive, source.size());
        return new ArrayList<>(source.subList(safeStart, safeEnd));
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private String chineseNumber(int value) {
        return switch (value) {
            case 1 -> "一";
            case 2 -> "二";
            case 3 -> "三";
            case 4 -> "四";
            case 5 -> "五";
            case 6 -> "六";
            case 7 -> "七";
            case 8 -> "八";
            case 9 -> "九";
            case 10 -> "十";
            default -> String.valueOf(value);
        };
    }

    private String colloquialChineseNumber(int value) {
        return switch (value) {
            case 2 -> "两";
            default -> chineseNumber(value);
        };
    }
}
