package com.recruit.agent.comparison.service.impl;

import com.recruit.agent.comparison.dto.CandidateComparisonRequest;
import com.recruit.agent.comparison.service.CandidateComparisonService;
import com.recruit.agent.comparison.vo.CandidateComparisonEvidenceVO;
import com.recruit.agent.comparison.vo.CandidateComparisonItemVO;
import com.recruit.agent.comparison.vo.CandidateComparisonResponse;
import com.recruit.agent.llm.LlmGenerationService;
import com.recruit.agent.rag.model.CandidateProfileIndex;
import com.recruit.agent.rag.model.ResumeChunk;
import com.recruit.agent.rag.repository.CandidateProfileIndexRepository;
import com.recruit.agent.rag.repository.ResumeChunkRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 候选人对比服务实现。
 */
@Service
public class CandidateComparisonServiceImpl implements CandidateComparisonService {

    private static final Logger log = LoggerFactory.getLogger(CandidateComparisonServiceImpl.class);

    private final CandidateProfileIndexRepository candidateProfileIndexRepository;
    private final ResumeChunkRepository resumeChunkRepository;
    private final LlmGenerationService llmGenerationService;

    public CandidateComparisonServiceImpl(CandidateProfileIndexRepository candidateProfileIndexRepository,
                                          ResumeChunkRepository resumeChunkRepository,
                                          LlmGenerationService llmGenerationService) {
        this.candidateProfileIndexRepository = candidateProfileIndexRepository;
        this.resumeChunkRepository = resumeChunkRepository;
        this.llmGenerationService = llmGenerationService;
    }

    @Override
    public CandidateComparisonResponse compare(CandidateComparisonRequest request) {
        CandidateComparisonRequest safeRequest = request == null ? new CandidateComparisonRequest() : request;
        List<CandidateComparisonItemVO> candidates = safeRequest.getCandidateIds().stream()
            .map(this::buildComparisonItem)
            .filter(Objects::nonNull)
            .toList();
        assignComparisonRanks(candidates);

        CandidateComparisonResponse response = new CandidateComparisonResponse();
        response.setTargetQuery(safeRequest.getTargetQuery());
        response.setCandidates(candidates);
        response.setSummary(buildSummaryWithLlm(candidates, safeRequest.getTargetQuery()));
        return response;
    }

    private void assignComparisonRanks(List<CandidateComparisonItemVO> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        for (int index = 0; index < candidates.size(); index++) {
            candidates.get(index).setRank(index + 1);
        }
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
            highlights.add("技术栈覆盖: " + String.join(", ", profile.getTechnicalSkills()));
        }
        if (Boolean.TRUE.equals(profile.getBigTech())) {
            highlights.add("具备大厂背景");
        }
        if (profile.getTotalYearsOfExperience() != null) {
            highlights.add("工作年限: " + profile.getTotalYearsOfExperience() + " 年");
        }
        if (profile.getSchoolTier() != null) {
            highlights.add("学校层级: " + profile.getSchoolTier());
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
            ? "当前候选人中"
            : "针对需求\"" + targetQuery + "\"，";

        return queryPart
            + bestExperience.getFullName()
            + " 在年限或背景维度上更突出；建议结合技术栈覆盖与风险点继续筛选。";
    }

    private String buildSummaryWithLlm(List<CandidateComparisonItemVO> candidates, String targetQuery) {
        String fallback = buildSummary(candidates, targetQuery);
        if (candidates.isEmpty()) {
            return fallback;
        }
        if (!llmGenerationService.isAvailable()) {
            log.info("Comparison summary uses deterministic fallback because LLM service is unavailable.");
            return fallback;
        }
        try {
            String summary = llmGenerationService.generate(
                buildComparisonSystemPrompt(),
                buildComparisonUserPrompt(candidates, targetQuery)
            );
            log.info("Comparison summary generated by LLM.");
            return summary == null || summary.isBlank() ? fallback : summary.trim();
        } catch (RuntimeException exception) {
            log.warn("Failed to generate comparison summary with LLM. Fallback to rule summary.", exception);
            return fallback;
        }
    }

    private String buildComparisonSystemPrompt() {
        return """
            你是招聘场景的候选人对比总结助手。
            你的任务是基于结构化候选人对比数据，生成一段简洁、专业、可执行的中文总结。
            要求：
            1. 不要编造不存在的经历或技能。
            2. 只基于输入中的亮点、风险点、年限、学历、学校层级和技术栈做总结。
            3. 总结控制在 80 到 140 字之间。
            4. 如果存在明显风险点，需要明确指出。
            """;
    }

    private String buildComparisonUserPrompt(List<CandidateComparisonItemVO> candidates, String targetQuery) {
        StringBuilder builder = new StringBuilder();
        builder.append("目标需求: ").append(targetQuery == null || targetQuery.isBlank() ? "未提供" : targetQuery).append('\n');
        for (CandidateComparisonItemVO candidate : candidates) {
            builder.append("候选人: ").append(safe(candidate.getFullName())).append('\n');
            builder.append("年限: ").append(candidate.getTotalYearsOfExperience()).append('\n');
            builder.append("学历: ").append(safe(candidate.getHighestDegree())).append('\n');
            builder.append("学校层级: ").append(safe(candidate.getSchoolTier())).append('\n');
            builder.append("技术栈: ").append(join(candidate.getTechnicalSkills())).append('\n');
            builder.append("亮点: ").append(join(candidate.getHighlights())).append('\n');
            builder.append("风险点: ").append(join(candidate.getRiskPoints())).append("\n\n");
        }
        return builder.toString().trim();
    }

    private String join(List<String> values) {
        return values == null || values.isEmpty() ? "无" : String.join("；", values);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "无" : value;
    }
}
