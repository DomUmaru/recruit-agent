package com.recruit.agent.comparison.service.impl;

import com.recruit.agent.comparison.dto.CandidateComparisonRequest;
import com.recruit.agent.comparison.service.CandidateComparisonService;
import com.recruit.agent.comparison.vo.CandidateComparisonEvidenceVO;
import com.recruit.agent.comparison.vo.CandidateComparisonItemVO;
import com.recruit.agent.comparison.vo.CandidateComparisonResponse;
import com.recruit.agent.rag.model.CandidateProfileIndex;
import com.recruit.agent.rag.model.ResumeChunk;
import com.recruit.agent.rag.repository.CandidateProfileIndexRepository;
import com.recruit.agent.rag.repository.ResumeChunkRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 候选人对比服务实现。
 */
@Service
public class CandidateComparisonServiceImpl implements CandidateComparisonService {

    private final CandidateProfileIndexRepository candidateProfileIndexRepository;
    private final ResumeChunkRepository resumeChunkRepository;

    public CandidateComparisonServiceImpl(CandidateProfileIndexRepository candidateProfileIndexRepository,
                                          ResumeChunkRepository resumeChunkRepository) {
        this.candidateProfileIndexRepository = candidateProfileIndexRepository;
        this.resumeChunkRepository = resumeChunkRepository;
    }

    @Override
    public CandidateComparisonResponse compare(CandidateComparisonRequest request) {
        CandidateComparisonRequest safeRequest = request == null ? new CandidateComparisonRequest() : request;
        List<CandidateComparisonItemVO> candidates = safeRequest.getCandidateIds().stream()
            .map(this::buildComparisonItem)
            .filter(Objects::nonNull)
            .toList();

        CandidateComparisonResponse response = new CandidateComparisonResponse();
        response.setTargetQuery(safeRequest.getTargetQuery());
        response.setCandidates(candidates);
        response.setSummary(buildSummary(candidates, safeRequest.getTargetQuery()));
        return response;
    }

    private CandidateComparisonItemVO buildComparisonItem(String candidateId) {
        CandidateProfileIndex profile = candidateProfileIndexRepository.findByCandidateId(candidateId).orElse(null);
        if (profile == null) {
            return null;
        }

        CandidateComparisonItemVO item = new CandidateComparisonItemVO();
        item.setCandidateId(profile.getCandidateId());
        item.setCandidateNo(profile.getCandidateNo());
        item.setFullName(profile.getFullName());
        item.setHighestDegree(profile.getHighestDegree());
        item.setSchoolTier(profile.getSchoolTier());
        item.setTotalYearsOfExperience(profile.getTotalYearsOfExperience());
        item.setTechnicalSkills(profile.getTechnicalSkills());
        item.setBigTech(profile.getBigTech());
        item.setOutsourcing(profile.getOutsourcing());
        item.setHighlights(buildHighlights(profile));
        item.setRiskPoints(buildRiskPoints(profile));
        item.setEvidenceList(loadEvidence(candidateId));
        return item;
    }

    private List<String> buildHighlights(CandidateProfileIndex profile) {
        List<String> highlights = new ArrayList<>();
        if (profile.getTechnicalSkills() != null && !profile.getTechnicalSkills().isEmpty()) {
            highlights.add("技术栈覆盖：" + String.join(", ", profile.getTechnicalSkills()));
        }
        if (Boolean.TRUE.equals(profile.getBigTech())) {
            highlights.add("具备大厂背景");
        }
        if (profile.getTotalYearsOfExperience() != null) {
            highlights.add("工作年限：" + profile.getTotalYearsOfExperience() + " 年");
        }
        if (profile.getSchoolTier() != null) {
            highlights.add("学校层级：" + profile.getSchoolTier());
        }
        return highlights;
    }

    private List<String> buildRiskPoints(CandidateProfileIndex profile) {
        List<String> risks = new ArrayList<>();
        if (Boolean.TRUE.equals(profile.getOutsourcing())) {
            risks.add("存在外包/驻场标签");
        }
        if (profile.getTechnicalSkills() == null || profile.getTechnicalSkills().size() < 2) {
            risks.add("技术栈信息较少");
        }
        if (profile.getTotalYearsOfExperience() == null) {
            risks.add("工作年限信息缺失");
        }
        if (profile.getSchoolTier() == null) {
            risks.add("学校层级信息缺失");
        }
        return risks;
    }

    private List<CandidateComparisonEvidenceVO> loadEvidence(String candidateId) {
        return resumeChunkRepository.findByCandidateId(candidateId).stream()
            .sorted(Comparator.comparing(ResumeChunk::getChunkOrder, Comparator.nullsLast(Integer::compareTo)))
            .limit(3)
            .map(this::toEvidence)
            .toList();
    }

    private CandidateComparisonEvidenceVO toEvidence(ResumeChunk chunk) {
        CandidateComparisonEvidenceVO evidence = new CandidateComparisonEvidenceVO();
        evidence.setSection(chunk.getSection());
        evidence.setPage(chunk.getPage());
        evidence.setContent(chunk.getContent());
        return evidence;
    }

    private String buildSummary(List<CandidateComparisonItemVO> candidates, String targetQuery) {
        if (candidates.isEmpty()) {
            return "未找到可对比的候选人。";
        }

        CandidateComparisonItemVO bestExperience = candidates.stream()
            .filter(candidate -> candidate.getTotalYearsOfExperience() != null)
            .max(Comparator.comparing(CandidateComparisonItemVO::getTotalYearsOfExperience))
            .orElse(candidates.get(0));

        String queryPart = targetQuery == null || targetQuery.isBlank()
            ? "当前候选人池"
            : "针对需求“" + targetQuery + "”";

        return queryPart + " 中，"
            + bestExperience.getFullName()
            + " 在年限或背景维度上更突出；建议结合技术栈覆盖与风险点继续筛选。";
    }
}
