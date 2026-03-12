package com.recruit.agent.search.reason.impl;

import com.recruit.agent.rag.model.CandidateProfileIndex;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import com.recruit.agent.search.reason.CandidateMatchReasonService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DefaultCandidateMatchReasonService implements CandidateMatchReasonService {

    @Override
    public List<String> buildMatchReasons(CandidateProfileIndex profile,
                                          String query,
                                          List<String> queryTerms,
                                          CandidateSearchFilter filter) {
        List<String> reasons = new ArrayList<>();

        if (!query.isBlank() && containsAny(normalize(query), profile.getTechnicalSkills())) {
            reasons.add("技术栈命中查询关键词");
        }
        if (!query.isBlank() && containsAny(normalize(query), profile.getProjectTags())) {
            reasons.add("项目标签命中查询关键词");
        }
        if (!query.isBlank() && normalize(profile.getProfileSummary()).contains(normalize(query))) {
            reasons.add("候选人摘要与查询语义匹配");
        }
        if (filter.getMinYearsOfExperience() != null && profile.getTotalYearsOfExperience() != null) {
            reasons.add("工作年限满足筛选条件");
        }
        if (Boolean.TRUE.equals(filter.getBigTech()) && Boolean.TRUE.equals(profile.getBigTech())) {
            reasons.add("具备大厂背景");
        }
        if (filter.getTechnicalSkills() != null && !filter.getTechnicalSkills().isEmpty()) {
            reasons.add("满足技术栈过滤条件");
        }

        if (reasons.isEmpty() && !queryTerms.isEmpty()) {
            reasons.add("基础关键词匹配");
        }
        if (reasons.isEmpty()) {
            reasons.add("满足结构化过滤条件");
        }
        return reasons;
    }

    private boolean containsAny(String queryTerm, List<String> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        return values.stream()
            .filter(Objects::nonNull)
            .map(this::normalize)
            .anyMatch(value -> value.contains(queryTerm));
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
}
