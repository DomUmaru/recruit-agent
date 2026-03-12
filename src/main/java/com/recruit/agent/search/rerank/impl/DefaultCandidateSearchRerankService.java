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

        List<String> documents = candidates.stream()
            .map(this::buildRerankDocument)
            .toList();

        try {
            List<Double> scores = rerankService.rerank(safeQuery, documents);
            if (scores.size() != candidates.size()) {
                log.warn("Rerank result size mismatch. expected={}, actual={}", candidates.size(), scores.size());
                return candidates;
            }

            List<CandidateSearchItemVO> reranked = new ArrayList<>(candidates);
            for (int index = 0; index < reranked.size(); index++) {
                reranked.get(index).setRerankScore(scores.get(index));
            }
            reranked.sort(Comparator
                .comparing(CandidateSearchItemVO::getRerankScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CandidateSearchItemVO::getMatchScore, Comparator.reverseOrder()));
            return reranked;
        } catch (IOException exception) {
            log.warn("Failed to rerank candidate search results. Fallback to original ES order.", exception);
            return candidates;
        }
    }

    private String buildRerankDocument(CandidateSearchItemVO candidate) {
        StringBuilder builder = new StringBuilder();
        append(builder, candidate.getFullName());
        append(builder, candidate.getCurrentCity());
        append(builder, candidate.getProfileSummary());
        if (candidate.getTechnicalSkills() != null && !candidate.getTechnicalSkills().isEmpty()) {
            append(builder, String.join(", ", candidate.getTechnicalSkills()));
        }
        if (candidate.getEvidenceList() != null) {
            candidate.getEvidenceList().stream()
                .map(CandidateSearchEvidenceVO::getContent)
                .filter(content -> content != null && !content.isBlank())
                .limit(2)
                .forEach(content -> append(builder, content));
        }
        return builder.toString().trim();
    }

    private void append(StringBuilder builder, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(text.toLowerCase(Locale.ROOT));
    }
}
