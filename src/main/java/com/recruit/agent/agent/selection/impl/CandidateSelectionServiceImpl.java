package com.recruit.agent.agent.selection.impl;

import com.recruit.agent.agent.selection.CandidateSelectionService;
import com.recruit.agent.chat.state.ChatSessionState;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * 基于规则的候选人选择解析器。
 * 这一层只处理 compare / interview 前置的“选人”语义，例如：
 * - 前两个
 * - 第一个和第三个
 * - 全部
 *
 * 它不负责搜索，也不负责真正的对比/出题，只负责把自然语言映射成 selectedCandidateIds。
 */
@Service
public class CandidateSelectionServiceImpl implements CandidateSelectionService {

    private static final Pattern TOP_N_PATTERN = Pattern.compile("前([0-9一二两三四五六七八九十俩]+)(个|位)?");
    private static final Pattern ORDINAL_PATTERN = Pattern.compile("第([0-9一二两三四五六七八九十俩]+)(个|位)?");

    @Override
    public List<String> resolveSelectedCandidateIds(ChatSessionState state, String userInput) {
        // 没有历史候选集时，无法做“第几个/前几个”这类选择，直接返回 null 交由上层决定是否降级。
        if (state == null || userInput == null || userInput.isBlank()) {
            return null;
        }

        List<String> lastCandidateIds = state.getLastCandidateIds();
        if (lastCandidateIds == null || lastCandidateIds.isEmpty()) {
            return null;
        }

        String normalized = normalize(userInput);

        // “全部”表示直接选中上一轮整个候选集。
        if (containsAny(normalized, "全部", "所有人", "所有候选人", "全部候选人", "all")) {
            return lastCandidateIds;
        }

        // “这两个人”优先复用已选集合，否则回退到上一轮结果集前两位。
        if (containsAny(normalized, "这两个人", "这两个候选人", "这两位", "这俩")) {
            if (state.getSelectedCandidateIds() != null && state.getSelectedCandidateIds().size() >= 2) {
                return new ArrayList<>(state.getSelectedCandidateIds().subList(0, 2));
            }
            return pickRange(lastCandidateIds, 0, 2);
        }

        // “这个人”语义类似，优先基于当前 selectedCandidateIds，再回退到上轮首位。
        if (containsAny(normalized, "这个人", "这位", "这个候选人")) {
            if (state.getSelectedCandidateIds() != null && !state.getSelectedCandidateIds().isEmpty()) {
                return List.of(state.getSelectedCandidateIds().get(0));
            }
            return pickRange(lastCandidateIds, 0, 1);
        }

        Integer topN = extractTopN(normalized);
        List<String> scopedCandidates = topN == null ? lastCandidateIds : pickRange(lastCandidateIds, 0, topN);

        // “第一个、第三个”是对 scopedCandidates 的 ordinal 选择。
        List<Integer> ordinals = extractOrdinals(normalized);
        if (!ordinals.isEmpty()) {
            return pickOrdinals(scopedCandidates, ordinals);
        }

        if (topN != null) {
            return scopedCandidates;
        }

        return null;
    }

    private Integer extractTopN(String text) {
        Matcher matcher = TOP_N_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return parseNumberToken(matcher.group(1));
    }

    private List<Integer> extractOrdinals(String text) {
        List<Integer> ordinals = new ArrayList<>();
        Matcher matcher = ORDINAL_PATTERN.matcher(text);
        while (matcher.find()) {
            Integer value = parseNumberToken(matcher.group(1));
            if (value != null) {
                ordinals.add(value);
            }
        }
        return ordinals;
    }

    private Integer parseNumberToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        if (token.chars().allMatch(Character::isDigit)) {
            return Integer.parseInt(token);
        }

        return switch (token) {
            case "一" -> 1;
            case "二", "两", "俩" -> 2;
            case "三" -> 3;
            case "四" -> 4;
            case "五" -> 5;
            case "六" -> 6;
            case "七" -> 7;
            case "八" -> 8;
            case "九" -> 9;
            case "十" -> 10;
            default -> null;
        };
    }

    private List<String> pickOrdinals(List<String> source, List<Integer> ordinals) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        // 用 LinkedHashSet 去重并保持用户表达中的顺序，避免“第一个和第一个”这类重复结果。
        Set<String> result = new LinkedHashSet<>();
        for (Integer ordinal : ordinals) {
            if (ordinal == null || ordinal <= 0 || ordinal > source.size()) {
                continue;
            }
            result.add(source.get(ordinal - 1));
        }
        return new ArrayList<>(result);
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
}
