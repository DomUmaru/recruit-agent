package com.recruit.agent.search.service.impl;

import com.recruit.agent.search.dto.CandidateSearchFilter;
import com.recruit.agent.search.dto.CandidateSearchRefineRequest;
import com.recruit.agent.search.dto.CandidateSearchRequest;
import com.recruit.agent.search.dto.FilterMergeMode;
import com.recruit.agent.search.parser.NaturalLanguageSearchFilterParser;
import com.recruit.agent.search.service.CandidateSearchRefinementService;
import com.recruit.agent.search.service.CandidateSearchService;
import com.recruit.agent.search.vo.CandidateSearchResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 候选人搜索 refinement 服务实现。
 */
@Service
public class CandidateSearchRefinementServiceImpl implements CandidateSearchRefinementService {

    private final CandidateSearchService candidateSearchService;
    private final NaturalLanguageSearchFilterParser naturalLanguageSearchFilterParser;

    public CandidateSearchRefinementServiceImpl(CandidateSearchService candidateSearchService,
                                                NaturalLanguageSearchFilterParser naturalLanguageSearchFilterParser) {
        this.candidateSearchService = candidateSearchService;
        this.naturalLanguageSearchFilterParser = naturalLanguageSearchFilterParser;
    }

    @Override
    public CandidateSearchRequest merge(CandidateSearchRefineRequest request) {
        CandidateSearchRefineRequest safeRequest = request == null ? new CandidateSearchRefineRequest() : request;
        CandidateSearchRequest baseRequest = safeRequest.getBaseRequest() == null ? new CandidateSearchRequest() : safeRequest.getBaseRequest();
        CandidateSearchFilter baseFilter = copyFilter(baseRequest.getFilter());
        CandidateSearchFilter parsedRefinementFilter = naturalLanguageSearchFilterParser.parse(safeRequest.getRefinementQuery());
        CandidateSearchFilter refinementFilter = mergeFilter(
            safeFilter(safeRequest.getRefinementFilter()),
            safeFilter(parsedRefinementFilter),
            FilterMergeMode.APPEND
        );
        FilterMergeMode mergeMode = safeRequest.getMergeMode() == null ? FilterMergeMode.APPEND : safeRequest.getMergeMode();

        CandidateSearchRequest merged = new CandidateSearchRequest();
        merged.setQuery(mergeQuery(baseRequest.getQuery(), safeRequest.getRefinementQuery(), mergeMode));
        merged.setFilter(mergeFilter(baseFilter, refinementFilter, mergeMode));
        merged.setScopeCandidateIds(resolveScope(baseRequest.getScopeCandidateIds(), safeRequest.getScopeCandidateIds()));
        merged.setLimit(baseRequest.getLimit());
        merged.setEvidenceLimit(baseRequest.getEvidenceLimit());
        return merged;
    }

    @Override
    public CandidateSearchResponse refineSearch(CandidateSearchRefineRequest request) {
        return candidateSearchService.search(merge(request));
    }

    private String mergeQuery(String baseQuery, String refinementQuery, FilterMergeMode mergeMode) {
        String normalizedBase = safeText(baseQuery).trim();
        String normalizedRefinement = safeText(naturalLanguageSearchFilterParser.stripFilterTerms(refinementQuery)).trim();

        if (normalizedRefinement.isBlank()) {
            return normalizedBase;
        }
        if (mergeMode == FilterMergeMode.REPLACE || normalizedBase.isBlank()) {
            return normalizedRefinement;
        }
        return normalizedBase + " " + normalizedRefinement;
    }

    private CandidateSearchFilter mergeFilter(CandidateSearchFilter baseFilter,
                                              CandidateSearchFilter refinementFilter,
                                              FilterMergeMode mergeMode) {
        if (mergeMode == FilterMergeMode.REPLACE) {
            return copyFilter(refinementFilter);
        }

        CandidateSearchFilter merged = copyFilter(baseFilter);
        merged.setHighestDegrees(mergeList(baseFilter.getHighestDegrees(), refinementFilter.getHighestDegrees()));
        merged.setSchoolTiers(mergeList(baseFilter.getSchoolTiers(), refinementFilter.getSchoolTiers()));
        merged.setTechnicalSkills(mergeList(baseFilter.getTechnicalSkills(), refinementFilter.getTechnicalSkills()));
        merged.setMinYearsOfExperience(max(baseFilter.getMinYearsOfExperience(), refinementFilter.getMinYearsOfExperience()));

        if (hasText(refinementFilter.getCurrentCity())) {
            merged.setCurrentCity(refinementFilter.getCurrentCity());
        }
        if (refinementFilter.getBigTech() != null) {
            merged.setBigTech(refinementFilter.getBigTech());
        }
        if (refinementFilter.getOutsourcing() != null) {
            merged.setOutsourcing(refinementFilter.getOutsourcing());
        }
        return merged;
    }

    private CandidateSearchFilter safeFilter(CandidateSearchFilter filter) {
        return filter == null ? new CandidateSearchFilter() : filter;
    }

    private CandidateSearchFilter copyFilter(CandidateSearchFilter filter) {
        CandidateSearchFilter source = safeFilter(filter);
        CandidateSearchFilter copy = new CandidateSearchFilter();
        copy.setHighestDegrees(copyList(source.getHighestDegrees()));
        copy.setSchoolTiers(copyList(source.getSchoolTiers()));
        copy.setMinYearsOfExperience(source.getMinYearsOfExperience());
        copy.setTechnicalSkills(copyList(source.getTechnicalSkills()));
        copy.setCurrentCity(source.getCurrentCity());
        copy.setBigTech(source.getBigTech());
        copy.setOutsourcing(source.getOutsourcing());
        return copy;
    }

    private <T> List<T> mergeList(List<T> base, List<T> refinement) {
        if (refinement == null || refinement.isEmpty()) {
            return copyList(base);
        }
        LinkedHashSet<T> values = new LinkedHashSet<>();
        if (base != null) {
            values.addAll(base);
        }
        values.addAll(refinement);
        return new ArrayList<>(values);
    }

    private <T> List<T> copyList(List<T> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return new ArrayList<>(values);
    }

    private List<String> resolveScope(List<String> baseScope, List<String> refinementScope) {
        if (refinementScope != null && !refinementScope.isEmpty()) {
            return new ArrayList<>(new LinkedHashSet<>(refinementScope));
        }
        if (baseScope == null || baseScope.isEmpty()) {
            return null;
        }
        return new ArrayList<>(new LinkedHashSet<>(baseScope));
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

    private boolean hasText(String text) {
        return !safeText(text).isBlank();
    }

    private String safeText(String text) {
        return Objects.toString(text, "");
    }
}
