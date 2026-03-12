package com.recruit.agent.search.normalization.impl;

import com.recruit.agent.search.dto.CandidateSearchFilter;
import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.normalization.PreparedCandidateSearchRequest;
import com.recruit.agent.search.normalization.SearchRequestNormalizationService;
import com.recruit.agent.search.parser.NaturalLanguageSearchFilterParser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 搜索请求归一化服务实现。
 */
@Service
public class SearchRequestNormalizationServiceImpl implements SearchRequestNormalizationService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int DEFAULT_EVIDENCE_LIMIT = 3;

    private final NaturalLanguageSearchFilterParser naturalLanguageSearchFilterParser;

    public SearchRequestNormalizationServiceImpl(NaturalLanguageSearchFilterParser naturalLanguageSearchFilterParser) {
        this.naturalLanguageSearchFilterParser = naturalLanguageSearchFilterParser;
    }

    @Override
    public PreparedCandidateSearchRequest prepare(CandidateSearchRequest request) {
        CandidateSearchRequest safeRequest = request == null ? new CandidateSearchRequest() : request;
        String rawQuery = safeText(safeRequest.getQuery());
        CandidateSearchFilter explicitFilter = safeRequest.getFilter() == null ? new CandidateSearchFilter() : safeRequest.getFilter();
        CandidateSearchFilter parsedFilter = naturalLanguageSearchFilterParser.parse(rawQuery);

        PreparedCandidateSearchRequest prepared = new PreparedCandidateSearchRequest();
        prepared.setRawQuery(rawQuery);
        prepared.setQuery(safeText(naturalLanguageSearchFilterParser.stripFilterTerms(rawQuery)));
        prepared.setFilter(mergeFilter(explicitFilter, parsedFilter));
        prepared.setScopeCandidateIds(safeRequest.getScopeCandidateIds());
        prepared.setLimit(normalizeCandidateLimit(safeRequest.getLimit()));
        prepared.setEvidenceLimit(normalizeEvidenceLimit(safeRequest.getEvidenceLimit()));
        return prepared;
    }

    private CandidateSearchFilter mergeFilter(CandidateSearchFilter explicitFilter, CandidateSearchFilter parsedFilter) {
        CandidateSearchFilter merged = new CandidateSearchFilter();
        merged.setHighestDegrees(mergeList(
            explicitFilter == null ? null : explicitFilter.getHighestDegrees(),
            parsedFilter == null ? null : parsedFilter.getHighestDegrees()
        ));
        merged.setSchoolTiers(mergeList(
            explicitFilter == null ? null : explicitFilter.getSchoolTiers(),
            parsedFilter == null ? null : parsedFilter.getSchoolTiers()
        ));
        merged.setTechnicalSkills(mergeList(
            explicitFilter == null ? null : explicitFilter.getTechnicalSkills(),
            parsedFilter == null ? null : parsedFilter.getTechnicalSkills()
        ));
        merged.setMinYearsOfExperience(max(
            explicitFilter == null ? null : explicitFilter.getMinYearsOfExperience(),
            parsedFilter == null ? null : parsedFilter.getMinYearsOfExperience()
        ));
        merged.setCurrentCity(hasText(explicitFilter == null ? null : explicitFilter.getCurrentCity())
            ? explicitFilter.getCurrentCity()
            : parsedFilter == null ? null : parsedFilter.getCurrentCity());
        merged.setBigTech(explicitFilter != null && explicitFilter.getBigTech() != null
            ? explicitFilter.getBigTech()
            : parsedFilter == null ? null : parsedFilter.getBigTech());
        merged.setOutsourcing(explicitFilter != null && explicitFilter.getOutsourcing() != null
            ? explicitFilter.getOutsourcing()
            : parsedFilter == null ? null : parsedFilter.getOutsourcing());
        return merged;
    }

    private <T> List<T> mergeList(List<T> left, List<T> right) {
        if ((left == null || left.isEmpty()) && (right == null || right.isEmpty())) {
            return null;
        }
        List<T> merged = new ArrayList<>();
        if (left != null) {
            merged.addAll(left);
        }
        if (right != null) {
            for (T value : right) {
                if (!merged.contains(value)) {
                    merged.add(value);
                }
            }
        }
        return merged;
    }

    private BigDecimal max(BigDecimal left, BigDecimal right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.compareTo(right) >= 0 ? left : right;
    }

    private int normalizeCandidateLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return limit;
    }

    private int normalizeEvidenceLimit(Integer limit) {
        if (limit == null || limit < 0) {
            return DEFAULT_EVIDENCE_LIMIT;
        }
        return limit;
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }
}
