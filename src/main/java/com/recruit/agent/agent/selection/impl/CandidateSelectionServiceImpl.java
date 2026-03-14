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
 * Rule-based candidate selector for compare/interview follow-up turns.
 */
@Service
public class CandidateSelectionServiceImpl implements CandidateSelectionService {

    private static final Pattern TOP_N_PATTERN = Pattern.compile("(前|top)\\s*([0-9一二两三四五六七八九十俩]+)\\s*(个|位|名)?");
    private static final Pattern ORDINAL_PATTERN = Pattern.compile("第\\s*([0-9一二两三四五六七八九十俩]+)\\s*(个|位|名)?");
    private static final Pattern LIST_SELECTION_PATTERN = Pattern.compile(
        "(?:第)?\\s*((?:[0-9一二两三四五六七八九十俩]+)(?:\\s*[、,，和及]\\s*(?:第)?\\s*[0-9一二两三四五六七八九十俩]+)+)\\s*(?:个|位|名|号|候选人)?"
    );
    private static final Pattern LAST_N_PATTERN = Pattern.compile("最后\\s*([0-9一二两三四五六七八九十俩]+)\\s*(个|位|名)?");

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

        if (containsAny(normalized, "这两个人", "这两位", "这两名候选人", "这俩")) {
            if (state.getSelectedCandidateIds() != null && state.getSelectedCandidateIds().size() >= 2) {
                return new ArrayList<>(state.getSelectedCandidateIds().subList(0, 2));
            }
            return pickRange(lastCandidateIds, 0, 2);
        }

        if (containsAny(normalized, "这个人", "这位", "这个候选人")) {
            if (state.getSelectedCandidateIds() != null && !state.getSelectedCandidateIds().isEmpty()) {
                return List.of(state.getSelectedCandidateIds().get(0));
            }
            return pickRange(lastCandidateIds, 0, 1);
        }

        Integer lastN = extractLastN(normalized);
        if (lastN != null) {
            return pickLastN(lastCandidateIds, lastN);
        }

        List<Integer> listedOrdinals = extractListedOrdinals(normalized);
        if (!listedOrdinals.isEmpty()) {
            return pickOrdinals(lastCandidateIds, listedOrdinals);
        }

        Integer topN = extractTopN(normalized);
        List<String> scopedCandidates = topN == null ? lastCandidateIds : pickRange(lastCandidateIds, 0, topN);

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
        if (text.contains("前三")) {
            return 3;
        }
        if (text.contains("前两") || text.contains("前俩")) {
            return 2;
        }
        Matcher matcher = TOP_N_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return parseNumberToken(matcher.group(2));
    }

    private Integer extractLastN(String text) {
        Matcher matcher = LAST_N_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return parseNumberToken(matcher.group(1));
    }

    private List<Integer> extractListedOrdinals(String text) {
        if (!containsAny(text, "比较", "对比", "看", "出题", "面试", "候选人")) {
            return List.of();
        }
        Matcher matcher = LIST_SELECTION_PATTERN.matcher(text);
        if (!matcher.find()) {
            return List.of();
        }

        String group = matcher.group(1);
        if (group == null || group.isBlank()) {
            return List.of();
        }

        List<Integer> ordinals = new ArrayList<>();
        for (String token : group.split("\\s*[、,，和及]\\s*")) {
            String normalizedToken = token.replace("第", "").replace("号", "").trim();
            Integer value = parseNumberToken(normalizedToken);
            if (value != null) {
                ordinals.add(value);
            }
        }
        return ordinals;
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
            case "第一" -> 1;
            case "二", "两", "俩" -> 2;
            case "第二" -> 2;
            case "三" -> 3;
            case "第三" -> 3;
            case "四" -> 4;
            case "第四" -> 4;
            case "五" -> 5;
            case "第五" -> 5;
            case "六" -> 6;
            case "第六" -> 6;
            case "七" -> 7;
            case "第七" -> 7;
            case "八" -> 8;
            case "第八" -> 8;
            case "九" -> 9;
            case "第九" -> 9;
            case "十" -> 10;
            case "第十" -> 10;
            default -> null;
        };
    }

    private List<String> pickOrdinals(List<String> source, List<Integer> ordinals) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

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

    private List<String> pickLastN(List<String> source, int count) {
        if (source == null || source.isEmpty() || count <= 0) {
            return List.of();
        }
        int safeStart = Math.max(source.size() - count, 0);
        return new ArrayList<>(source.subList(safeStart, source.size()));
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
