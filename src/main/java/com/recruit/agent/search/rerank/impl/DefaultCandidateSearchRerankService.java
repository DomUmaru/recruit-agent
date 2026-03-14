package com.recruit.agent.search.rerank.impl;

import com.recruit.agent.position.model.PositionJD;
import com.recruit.agent.search.rerank.CandidateSearchRerankService;
import com.recruit.agent.search.rerank.RerankService;
import com.recruit.agent.search.vo.CandidateSearchEvidenceVO;
import com.recruit.agent.search.vo.CandidateSearchItemVO;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultCandidateSearchRerankService implements CandidateSearchRerankService {

    private static final Logger log = LoggerFactory.getLogger(DefaultCandidateSearchRerankService.class);

    private static final int RERANK_WINDOW_SIZE = 20;
    private static final int MAX_EVIDENCE_SNIPPETS = 3;

    private static final double RERANK_WEIGHT = 0.75d;
    private static final double PREFERENCE_WEIGHT = 0.10d;
    private static final double JD_PREFERENCE_WEIGHT = 0.15d;

    private final RerankService rerankService;

    public DefaultCandidateSearchRerankService(RerankService rerankService) {
        this.rerankService = rerankService;
    }

    @Override
    public List<CandidateSearchItemVO> rerank(String query, List<CandidateSearchItemVO> candidates, PositionJD positionJD) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        String safeQuery = query == null ? "" : query.trim();
        boolean earlyCareerIntent = isEarlyCareerIntent(safeQuery);
        if (safeQuery.isBlank() || !rerankService.isAvailable()) {
            return candidates;
        }

        List<CandidateSearchItemVO> rerankWindow = new ArrayList<>(candidates.stream().limit(RERANK_WINDOW_SIZE).toList());
        List<CandidateSearchItemVO> tail = new ArrayList<>(candidates.stream().skip(RERANK_WINDOW_SIZE).toList());
        List<String> documents = rerankWindow.stream().map(this::buildRerankDocument).toList();

        try {
            List<Double> scores = rerankService.rerank(safeQuery, documents);
            if (scores.size() != rerankWindow.size()) {
                log.warn("Rerank result size mismatch. expected={}, actual={}", rerankWindow.size(), scores.size());
                return candidates;
            }

            for (int index = 0; index < rerankWindow.size(); index++) {
                CandidateSearchItemVO candidate = rerankWindow.get(index);
                double rerankScore = normalizeScore(scores.get(index));
                double preferenceScore = computePreferenceScore(candidate, earlyCareerIntent);
                double jdPreferenceScore = computeJdPreferenceScore(candidate, positionJD);
                candidate.setRerankScore(rerankScore);
                candidate.setPreferenceScore(preferenceScore);
                candidate.setJdPreferenceScore(jdPreferenceScore);
                candidate.setFinalScore((rerankScore * RERANK_WEIGHT)
                    + (preferenceScore * PREFERENCE_WEIGHT)
                    + (jdPreferenceScore * JD_PREFERENCE_WEIGHT));
            }

            rerankWindow.sort(Comparator
                .comparing(CandidateSearchItemVO::getFinalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CandidateSearchItemVO::getRerankScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CandidateSearchItemVO::getJdPreferenceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CandidateSearchItemVO::getPreferenceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CandidateSearchItemVO::getMatchScore, Comparator.reverseOrder())
                .thenComparing(CandidateSearchItemVO::getCandidateNo, Comparator.nullsLast(String::compareTo)));
            rerankWindow.addAll(tail);
            return rerankWindow;
        } catch (IOException exception) {
            log.warn("Failed to rerank candidate search results. Fallback to original ES order.", exception);
            return candidates;
        }
    }

    private String buildRerankDocument(CandidateSearchItemVO candidate) {
        List<String> sections = new ArrayList<>();
        addSection(sections, "name", candidate.getFullName());
        addSection(sections, "city", candidate.getCurrentCity());
        addSection(sections, "degree", candidate.getHighestDegree());
        addSection(sections, "school_tier", candidate.getSchoolTier());
        if (candidate.getTotalYearsOfExperience() != null) {
            addSection(sections, "years", candidate.getTotalYearsOfExperience().stripTrailingZeros().toPlainString());
        }
        addSection(sections, "summary", candidate.getProfileSummary());
        if (candidate.getTechnicalSkills() != null && !candidate.getTechnicalSkills().isEmpty()) {
            addSection(sections, "skills", String.join(", ", candidate.getTechnicalSkills()));
        }
        if (candidate.getBigTech() != null) {
            addSection(sections, "big_tech", candidate.getBigTech() ? "yes" : "no");
        }
        if (candidate.getOutsourcing() != null) {
            addSection(sections, "outsourcing", candidate.getOutsourcing() ? "yes" : "no");
        }
        if (candidate.getEvidenceList() != null) {
            candidate.getEvidenceList().stream()
                .filter(evidence -> evidence.getContent() != null && !evidence.getContent().isBlank())
                .limit(MAX_EVIDENCE_SNIPPETS)
                .forEach(evidence -> addSection(sections, buildEvidenceLabel(evidence), evidence.getContent()));
        }
        return String.join("\n", sections).trim();
    }

    private void addSection(List<String> sections, String label, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        sections.add(label + ": " + text.toLowerCase(Locale.ROOT));
    }

    private String buildEvidenceLabel(CandidateSearchEvidenceVO evidence) {
        if (evidence.getSection() != null && !evidence.getSection().isBlank()) {
            return "evidence_" + evidence.getSection();
        }
        return "evidence";
    }

    private double normalizeScore(Double score) {
        return score != null && Double.isFinite(score) ? score : 0.0d;
    }

    private double computePreferenceScore(CandidateSearchItemVO candidate, boolean earlyCareerIntent) {
        double score = 0.0d;
        score += degreeScore(candidate.getHighestDegree());
        score += schoolTierScore(candidate.getSchoolTier());
        if (!earlyCareerIntent) {
            score += experienceScore(candidate.getTotalYearsOfExperience());
        }
        if (Boolean.TRUE.equals(candidate.getBigTech())) {
            score += 0.15d;
        }
        if (Boolean.FALSE.equals(candidate.getOutsourcing())) {
            score += 0.15d;
        }
        return Math.min(score, 1.0d);
    }

    private double computeJdPreferenceScore(CandidateSearchItemVO candidate, PositionJD positionJD) {
        if (positionJD == null) {
            return 0.0d;
        }
        String candidateText = buildCandidatePreferenceText(candidate);
        double score = 0.0d;
        score += titleDirectionScore(candidateText, positionJD.getTitle());
        score += skillCoverageScore(candidateText, positionJD.getPrioritySkills(), 0.45d, 3);
        score += skillCoverageScore(candidateText, positionJD.getBonusSkills(), 0.20d, 3);
        return Math.min(score, 1.0d);
    }

    private String buildCandidatePreferenceText(CandidateSearchItemVO candidate) {
        StringBuilder builder = new StringBuilder();
        append(builder, candidate.getProfileSummary());
        if (candidate.getTechnicalSkills() != null && !candidate.getTechnicalSkills().isEmpty()) {
            append(builder, String.join(" ", candidate.getTechnicalSkills()));
        }
        if (candidate.getEvidenceList() != null) {
            candidate.getEvidenceList().stream()
                .map(CandidateSearchEvidenceVO::getContent)
                .forEach(text -> append(builder, text));
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private double titleDirectionScore(String candidateText, String title) {
        if (title == null || title.isBlank()) {
            return 0.0d;
        }
        String normalizedTitle = title.toLowerCase(Locale.ROOT);
        double score = 0.0d;
        if (containsAny(normalizedTitle, "java") && candidateText.contains("java")) {
            score += 0.12d;
        }
        if (containsAny(normalizedTitle, "go", "golang") && containsAny(candidateText, "go", "golang", "grpc")) {
            score += 0.12d;
        }
        if (containsAny(normalizedTitle, "c++", "cpp") && containsAny(candidateText, "c++", "cpp")) {
            score += 0.12d;
        }
        if (containsAny(normalizedTitle, "前端", "react", "frontend") && containsAny(candidateText, "前端", "react", "vue", "typescript")) {
            score += 0.12d;
        }
        if (containsAny(normalizedTitle, "搜索", "检索") && containsAny(candidateText, "搜索", "检索", "elasticsearch")) {
            score += 0.15d;
        }
        if (containsAny(normalizedTitle, "推荐") && containsAny(candidateText, "推荐", "召回", "排序")) {
            score += 0.15d;
        }
        if (containsAny(normalizedTitle, "算法", "ai", "大模型", "llm") && containsAny(candidateText, "算法", "模型", "训练", "llm", "rag")) {
            score += 0.15d;
        }
        return Math.min(score, 0.25d);
    }

    private double skillCoverageScore(String candidateText, String rawSkills, double maxScore, int cap) {
        List<String> skills = splitSkills(rawSkills);
        if (skills.isEmpty()) {
            return 0.0d;
        }
        int matched = 0;
        for (String skill : skills) {
            if (candidateText.contains(skill.toLowerCase(Locale.ROOT))) {
                matched++;
            }
        }
        if (matched == 0) {
            return 0.0d;
        }
        double ratio = Math.min(matched, cap) / (double) Math.min(skills.size(), cap);
        return ratio * maxScore;
    }

    private List<String> splitSkills(String rawSkills) {
        if (rawSkills == null || rawSkills.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawSkills.split("[,，、/]"))
            .map(String::trim)
            .filter(skill -> !skill.isBlank())
            .toList();
    }

    private void append(StringBuilder builder, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        builder.append(text);
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private double degreeScore(String degree) {
        if (degree == null || degree.isBlank()) {
            return 0.0d;
        }
        return switch (degree.toUpperCase(Locale.ROOT)) {
            case "DOCTOR" -> 0.20d;
            case "MASTER" -> 0.16d;
            case "BACHELOR" -> 0.10d;
            case "ASSOCIATE" -> 0.04d;
            default -> 0.0d;
        };
    }

    private double schoolTierScore(String schoolTier) {
        if (schoolTier == null || schoolTier.isBlank()) {
            return 0.0d;
        }
        return switch (schoolTier.toUpperCase(Locale.ROOT)) {
            case "C9" -> 0.20d;
            case "PROJECT_985" -> 0.18d;
            case "PROJECT_211" -> 0.14d;
            case "DOUBLE_FIRST_CLASS" -> 0.12d;
            case "OVERSEAS_TOP" -> 0.18d;
            case "GENERAL_UNDERGRAD" -> 0.08d;
            case "JUNIOR_COLLEGE" -> 0.02d;
            default -> 0.0d;
        };
    }

    private double experienceScore(BigDecimal years) {
        if (years == null) {
            return 0.0d;
        }
        double safeYears = Math.max(0.0d, years.doubleValue());
        return Math.min(safeYears, 8.0d) / 8.0d * 0.14d;
    }

    private boolean isEarlyCareerIntent(String query) {
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT);
        return normalized.contains("校招")
            || normalized.contains("应届")
            || normalized.contains("学生")
            || normalized.contains("实习")
            || normalized.contains("无经验")
            || normalized.contains("毕业生");
    }
}
