package com.recruit.agent.search.rerank.impl;

import com.recruit.agent.search.rerank.CandidateSearchRerankService;
import com.recruit.agent.search.rerank.RerankService;
import com.recruit.agent.search.vo.CandidateSearchEvidenceVO;
import com.recruit.agent.search.vo.CandidateSearchItemVO;
import java.io.IOException;
import java.util.ArrayList;
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

    private final RerankService rerankService;

    public DefaultCandidateSearchRerankService(RerankService rerankService) {
        this.rerankService = rerankService;
    }

    @Override
    public List<CandidateSearchItemVO> rerank(String query, List<CandidateSearchItemVO> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        String safeQuery = query == null ? "" : query.trim();
        if (safeQuery.isBlank() || !rerankService.isAvailable()) {
            return candidates;
        }

        List<CandidateSearchItemVO> rerankWindow = new ArrayList<>(candidates.stream()
            .limit(RERANK_WINDOW_SIZE)
            .toList());
        List<CandidateSearchItemVO> tail = new ArrayList<>(candidates.stream()
            .skip(RERANK_WINDOW_SIZE)
            .toList());

        List<String> documents = rerankWindow.stream()
            .map(this::buildRerankDocument)
            .toList();

        try {
            List<Double> scores = rerankService.rerank(safeQuery, documents);
            if (scores.size() != rerankWindow.size()) {
                log.warn("Rerank result size mismatch. expected={}, actual={}", rerankWindow.size(), scores.size());
                return candidates;
            }

            for (int index = 0; index < rerankWindow.size(); index++) {
                rerankWindow.get(index).setRerankScore(normalizeScore(scores.get(index)));
            }
            rerankWindow.sort(Comparator
                .comparing(CandidateSearchItemVO::getRerankScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CandidateSearchItemVO::getMatchScore, Comparator.reverseOrder())
                .thenComparing(CandidateSearchItemVO::getCandidateNo, Comparator.nullsLast(String::compareTo)));
            rerankWindow.addAll(tail);
            List<CandidateSearchItemVO> reranked = rerankWindow;
            return reranked;
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

    private Double normalizeScore(Double score) {
        return score != null && Double.isFinite(score) ? score : 0.0d;
    }
}
