package com.recruit.agent.search.normalization.impl;

import com.recruit.agent.search.dto.CandidateSearchFilter;
import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.normalization.PreparedCandidateSearchRequest;
import com.recruit.agent.search.normalization.SearchRequestNormalizationService;
import com.recruit.agent.search.parser.NaturalLanguageSearchFilterParser;
import com.recruit.agent.search.parser.SearchIntentParseResult;
import com.recruit.agent.search.parser.SearchIntentParser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 搜索请求归一化服务实现。
 */
@Service
public class SearchRequestNormalizationServiceImpl implements SearchRequestNormalizationService {

    private static final Logger log = LoggerFactory.getLogger(SearchRequestNormalizationServiceImpl.class);

    private static final int DEFAULT_LIMIT = 10;
    private static final int DEFAULT_EVIDENCE_LIMIT = 3;

    private final NaturalLanguageSearchFilterParser naturalLanguageSearchFilterParser;
    private final SearchIntentParser searchIntentParser;

    public SearchRequestNormalizationServiceImpl(NaturalLanguageSearchFilterParser naturalLanguageSearchFilterParser) {
        this(naturalLanguageSearchFilterParser, null);
    }

    @Autowired
    public SearchRequestNormalizationServiceImpl(NaturalLanguageSearchFilterParser naturalLanguageSearchFilterParser,
                                                 SearchIntentParser searchIntentParser) {
        this.naturalLanguageSearchFilterParser = naturalLanguageSearchFilterParser;
        this.searchIntentParser = searchIntentParser;
    }

    @Override
    public PreparedCandidateSearchRequest prepare(CandidateSearchRequest request) {
        CandidateSearchRequest safeRequest = request == null ? new CandidateSearchRequest() : request;
        String rawQuery = safeText(safeRequest.getQuery());
        CandidateSearchFilter explicitFilter = safeRequest.getFilter() == null ? new CandidateSearchFilter() : safeRequest.getFilter();
        CandidateSearchFilter parsedFilter = naturalLanguageSearchFilterParser.parse(rawQuery);
        SearchIntentParseResult llmResult = parseWithLlm(rawQuery);
        CandidateSearchFilter mergedParsedFilter = mergeFilter(parsedFilter, llmResult.getFilter());

        PreparedCandidateSearchRequest prepared = new PreparedCandidateSearchRequest();
        prepared.setRawQuery(rawQuery);
        prepared.setQuery(resolveResidualQuery(rawQuery, llmResult));
        prepared.setFilter(mergeFilter(explicitFilter, mergedParsedFilter));
        prepared.setScopeCandidateIds(safeRequest.getScopeCandidateIds());
        prepared.setLimit(normalizeCandidateLimit(safeRequest.getLimit()));
        prepared.setEvidenceLimit(normalizeEvidenceLimit(safeRequest.getEvidenceLimit()));
        return prepared;
    }

    private SearchIntentParseResult parseWithLlm(String rawQuery) {
        if (searchIntentParser == null || !searchIntentParser.isAvailable()) {
            log.info("Search intent parser fallback: llm parser unavailable.");
            return new SearchIntentParseResult();
        }
        SearchIntentParseResult result = searchIntentParser.parse(rawQuery);
        log.info("Search intent parser used LLM. residualQuery='{}', filterPresent={}",
            result == null ? null : result.getResidualQuery(),
            result != null && result.getFilter() != null);
        return result == null ? new SearchIntentParseResult() : result;
    }

    private String resolveResidualQuery(String rawQuery, SearchIntentParseResult llmResult) {
        String llmResidual = llmResult == null ? "" : safeText(llmResult.getResidualQuery());
        if (!llmResidual.isBlank()) {
            log.info("Search normalization uses LLM residual query='{}'.", llmResidual);
            return llmResidual;
        }
        String fallbackResidual = safeText(naturalLanguageSearchFilterParser.stripFilterTerms(rawQuery));
        log.info("Search normalization falls back to rule residual query='{}'.", fallbackResidual);
        return fallbackResidual;
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
