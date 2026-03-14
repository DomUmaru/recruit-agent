package com.recruit.agent.interview.service.impl;

import com.recruit.agent.interview.dto.InterviewQuestionRequest;
import com.recruit.agent.interview.service.InterviewQuestionService;
import com.recruit.agent.interview.vo.CandidateInterviewQuestionVO;
import com.recruit.agent.interview.vo.InterviewQuestionEvidenceVO;
import com.recruit.agent.interview.vo.InterviewQuestionItemVO;
import com.recruit.agent.interview.vo.InterviewQuestionResponse;
import com.recruit.agent.llm.LlmGenerationService;
import com.recruit.agent.position.model.PositionJD;
import com.recruit.agent.position.repository.PositionJDRepository;
import com.recruit.agent.rag.model.CandidateProfileIndex;
import com.recruit.agent.rag.model.ResumeChunk;
import com.recruit.agent.rag.repository.CandidateProfileIndexRepository;
import com.recruit.agent.rag.repository.ResumeChunkRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Generate interview handoff briefs for technical interviewers.
 */
@Service
public class InterviewQuestionServiceImpl implements InterviewQuestionService {

    private static final Logger log = LoggerFactory.getLogger(InterviewQuestionServiceImpl.class);
    private static final int EVIDENCE_LIMIT = 10;
    private static final List<String> PRIORITY_SECTIONS = List.of("项目经历", "工作经验", "实习经历", "专业技能");

    private final CandidateProfileIndexRepository candidateProfileIndexRepository;
    private final ResumeChunkRepository resumeChunkRepository;
    private final PositionJDRepository positionJDRepository;
    private final LlmGenerationService llmGenerationService;

    public InterviewQuestionServiceImpl(CandidateProfileIndexRepository candidateProfileIndexRepository,
                                        ResumeChunkRepository resumeChunkRepository,
                                        PositionJDRepository positionJDRepository,
                                        LlmGenerationService llmGenerationService) {
        this.candidateProfileIndexRepository = candidateProfileIndexRepository;
        this.resumeChunkRepository = resumeChunkRepository;
        this.positionJDRepository = positionJDRepository;
        this.llmGenerationService = llmGenerationService;
    }

    @Override
    public InterviewQuestionResponse generate(InterviewQuestionRequest request) {
        InterviewQuestionRequest safeRequest = request == null ? new InterviewQuestionRequest() : request;
        PositionJD positionJD = resolvePositionJd(safeRequest.getPositionId()).orElse(null);

        List<CandidateInterviewQuestionVO> candidates = safeRequest.getCandidateIds() == null
            ? List.of()
            : safeRequest.getCandidateIds().stream()
                .map(candidateId -> buildCandidateQuestions(candidateId, safeRequest.getTargetQuery(), positionJD))
                .filter(Objects::nonNull)
                .toList();
        assignInterviewRanks(candidates);

        InterviewQuestionResponse response = new InterviewQuestionResponse();
        response.setTargetQuery(safeRequest.getTargetQuery());
        response.setCandidates(candidates);
        response.setSummary(buildSummary(candidates, safeRequest.getTargetQuery(), positionJD));
        return response;
    }

    private void assignInterviewRanks(List<CandidateInterviewQuestionVO> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        for (int index = 0; index < candidates.size(); index++) {
            candidates.get(index).setRank(index + 1);
        }
    }

    private CandidateInterviewQuestionVO buildCandidateQuestions(String candidateId,
                                                                 String targetQuery,
                                                                 PositionJD positionJD) {
        CandidateProfileIndex profile = candidateProfileIndexRepository.findByCandidateId(candidateId).orElse(null);
        if (profile == null) {
            return null;
        }

        List<ResumeChunk> evidenceChunks = selectEvidenceChunks(profile, targetQuery, positionJD);

        CandidateInterviewQuestionVO candidate = new CandidateInterviewQuestionVO();
        candidate.setCandidateId(profile.getCandidateId());
        candidate.setCandidateNo(profile.getCandidateNo());
        candidate.setFullName(profile.getFullName());
        candidate.setQuestions(buildHandoffItems(profile, evidenceChunks, targetQuery, positionJD));
        return candidate;
    }

    private List<ResumeChunk> selectEvidenceChunks(CandidateProfileIndex profile,
                                                   String targetQuery,
                                                   PositionJD positionJD) {
        List<String> keywords = mergeKeywords(
            tokenize(targetQuery),
            tokenize(positionJD == null ? null : positionJD.getPrioritySkills()),
            tokenize(positionJD == null ? null : positionJD.getBonusSkills()),
            profile.getTechnicalSkills() == null ? List.of() : profile.getTechnicalSkills()
        );

        List<ResumeChunk> preferred = resumeChunkRepository.findByCandidateId(profile.getCandidateId()).stream()
            .filter(this::hasUsableContent)
            .sorted(Comparator
                .comparingInt((ResumeChunk chunk) -> scoreChunk(chunk, keywords)).reversed()
                .thenComparing(ResumeChunk::getChunkOrder, Comparator.nullsLast(Integer::compareTo)))
            .limit(EVIDENCE_LIMIT)
            .toList();

        if (!preferred.isEmpty()) {
            return preferred;
        }

        return resumeChunkRepository.findByCandidateId(profile.getCandidateId()).stream()
            .filter(this::hasUsableContent)
            .sorted(Comparator.comparing(ResumeChunk::getChunkOrder, Comparator.nullsLast(Integer::compareTo)))
            .limit(3)
            .toList();
    }

    private boolean hasUsableContent(ResumeChunk chunk) {
        return chunk != null && chunk.getContent() != null && !chunk.getContent().isBlank();
    }

    private int scoreChunk(ResumeChunk chunk, List<String> keywords) {
        int score = sectionPriority(chunk.getSection()) * 100;
        String text = (safe(chunk.getSubSectionTitle()) + " " + safe(chunk.getContent())).toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            String normalizedKeyword = safe(keyword).toLowerCase(Locale.ROOT);
            if (!normalizedKeyword.isBlank() && text.contains(normalizedKeyword)) {
                score += 10;
            }
        }
        if (hasText(chunk.getSubSectionTitle())) {
            score += 5;
        }
        return score;
    }

    private int sectionPriority(String section) {
        if (section == null) {
            return 0;
        }
        for (int index = 0; index < PRIORITY_SECTIONS.size(); index++) {
            if (PRIORITY_SECTIONS.get(index).equals(section)) {
                return PRIORITY_SECTIONS.size() - index;
            }
        }
        return 0;
    }

    private List<InterviewQuestionItemVO> buildHandoffItems(CandidateProfileIndex profile,
                                                            List<ResumeChunk> evidenceChunks,
                                                            String targetQuery,
                                                            PositionJD positionJD) {
        List<InterviewQuestionItemVO> items = new ArrayList<>();
        ResumeChunk strongestChunk = evidenceChunks.isEmpty() ? null : evidenceChunks.get(0);
        String jdTitle = positionJD == null ? null : positionJD.getTitle();
        String prioritySkills = normalizeSkillText(positionJD == null ? null : positionJD.getPrioritySkills());
        String strongestSkill = resolveStrongestSkill(profile, positionJD);

        items.add(buildItem(
            "推荐理由",
            buildRecommendationQuestion(jdTitle, prioritySkills),
            "帮助技术面试官快速确认该候选人为什么通过 HR 初筛，以及最值得验证的匹配点。",
            evidenceChunks
        ));

        items.add(buildItem(
            "风险点/存疑点",
            buildRiskQuestion(profile, strongestChunk),
            "标出简历里最容易被高估或描述不完整的经历，便于二面时快速验证真实深度。",
            evidenceChunks
        ));

        items.add(buildItem(
            "追问建议-核心项目",
            buildProjectFollowUpQuestion(strongestChunk, targetQuery),
            "围绕最相关项目追问背景、方案、结果和个人贡献，避免只停留在技术栈罗列。",
            evidenceChunks
        ));

        items.add(buildItem(
            "追问建议-关键技术",
            buildSkillFollowUpQuestion(strongestSkill, prioritySkills),
            "结合岗位核心技能，对最关键的技术点做深挖，确认候选人是否真正具备落地能力。",
            evidenceChunks
        ));

        if (profile.getTotalYearsOfExperience() != null) {
            items.add(buildItem(
                "追问建议-经验复盘",
                buildExperienceQuestion(profile.getTotalYearsOfExperience()),
                "核验候选人的经验深度、技术取舍能力和推动结果的责任边界。",
                evidenceChunks
            ));
        }

        return items.stream().limit(5).toList();
    }

    private InterviewQuestionItemVO buildItem(String category,
                                              String question,
                                              String rationale,
                                              List<ResumeChunk> evidenceChunks) {
        InterviewQuestionItemVO item = new InterviewQuestionItemVO();
        item.setCategory(category);
        item.setQuestion(question);
        item.setRationale(rationale);
        item.setEvidenceList(evidenceChunks.stream().map(this::toEvidence).toList());
        return item;
    }

    private String buildRecommendationQuestion(String jdTitle, String prioritySkills) {
        String target = hasText(jdTitle) ? jdTitle : "当前岗位";
        if (hasText(prioritySkills)) {
            return "请先围绕候选人与\"" + target + "\"的贴合点展开确认：重点核实他在 "
                + prioritySkills + " 这些核心技能上的真实项目场景、复杂度和个人贡献。";
        }
        return "请先说明候选人为什么适合\"" + target + "\"，重点核实其最匹配的一段项目经历、承担角色和业务结果。";
    }

    private String buildRiskQuestion(CandidateProfileIndex profile, ResumeChunk strongestChunk) {
        if (Boolean.TRUE.equals(profile.getOutsourcing())) {
            return "简历中存在外包/驻场标签，请重点追问其真实汇报关系、决策边界，以及是否承担过核心模块责任。";
        }
        if (strongestChunk != null && !hasText(strongestChunk.getSubSectionTitle())) {
            return "这段经历缺少明确的项目标题或职责边界，请追问项目背景、输入输出和个人负责部分，避免只停留在概括描述。";
        }
        return "请从简历中最关键的一段项目经历切入，确认候选人描述的复杂度、产出指标和个人贡献是否足够具体。";
    }

    private String buildProjectFollowUpQuestion(ResumeChunk strongestChunk, String targetQuery) {
        String context = strongestChunk != null && hasText(strongestChunk.getSubSectionTitle())
            ? strongestChunk.getSubSectionTitle()
            : (hasText(targetQuery) ? targetQuery : "最相关项目");
        return "请围绕\"" + context + "\"这段经历继续深挖：项目目标是什么、你负责的核心模块是什么、技术方案为何这样设计、最终结果如何衡量？";
    }

    private String buildSkillFollowUpQuestion(String strongestSkill, String prioritySkills) {
        if (hasText(strongestSkill)) {
            return "请围绕 " + strongestSkill + " 做一次技术深挖：当时遇到的性能、稳定性或可维护性问题是什么，你做了哪些取舍，最终如何验证方案有效？";
        }
        if (hasText(prioritySkills)) {
            return "请在 " + prioritySkills + " 这些岗位核心技能中任选一项，结合具体项目说明你最复杂的一次落地实践和关键技术决策。";
        }
        return "请结合你的核心技术栈，举一个最复杂的工程实践案例，说明你如何识别问题、设计方案并验证结果。";
    }

    private String buildExperienceQuestion(BigDecimal years) {
        if (years.compareTo(new BigDecimal("5")) >= 0) {
            return "你有 " + years + " 年左右经验，请举例说明你如何做技术取舍、推动跨团队协作，并对最终结果负责。";
        }
        return "你目前有 " + years + " 年左右经验，请举一个成长最快的项目，说明你承担了哪些超出预期的职责，以及学到了什么。";
    }

    private InterviewQuestionEvidenceVO toEvidence(ResumeChunk chunk) {
        InterviewQuestionEvidenceVO evidence = new InterviewQuestionEvidenceVO();
        evidence.setSection(chunk.getSection());
        evidence.setPage(chunk.getPage());
        evidence.setContent(chunk.getContent());
        return evidence;
    }

    private String buildSummary(List<CandidateInterviewQuestionVO> candidates, String targetQuery, PositionJD positionJD) {
        if (candidates.isEmpty()) {
            return "未找到可生成面试交接提纲的候选人。";
        }
        CandidateInterviewQuestionVO candidate = candidates.get(0);
        List<InterviewQuestionEvidenceVO> evidenceList = candidate.getQuestions().stream()
            .flatMap(question -> question.getEvidenceList() == null ? java.util.stream.Stream.empty() : question.getEvidenceList().stream())
            .distinct()
            .limit(EVIDENCE_LIMIT)
            .toList();
        String target = hasText(positionJD == null ? null : positionJD.getTitle())
            ? positionJD.getTitle()
            : (hasText(targetQuery) ? targetQuery : "当前岗位");
        String recommendation = buildRecommendationSummary(candidate, target, evidenceList);
        String risk = buildRiskSummary(evidenceList);
        String followUp = buildFollowUpSummary(evidenceList);
        return """
            【推荐理由】
            %s
            【风险点/存疑点】
            %s
            【追问建议】
            %s
            """.formatted(recommendation, risk, followUp).trim();
    }

    private String buildRecommendationSummary(CandidateInterviewQuestionVO candidate,
                                              String target,
                                              List<InterviewQuestionEvidenceVO> evidenceList) {
        String roleEvidence = firstEvidenceContaining(evidenceList, "工作经验");
        String projectEvidence = firstEvidenceContaining(evidenceList, "项目经历");
        if (hasText(roleEvidence) && hasText(projectEvidence)) {
            return "候选人与\"" + target + "\"的匹配点主要来自这两段证据：" + compact(roleEvidence) + "；" + compact(projectEvidence) + "。";
        }
        if (hasText(roleEvidence)) {
            return "候选人与\"" + target + "\"的匹配点主要来自工作经历：" + compact(roleEvidence) + "。";
        }
        if (hasText(projectEvidence)) {
            return "候选人与\"" + target + "\"的匹配点主要来自项目经历：" + compact(projectEvidence) + "。";
        }
        return "当前可用证据主要说明候选人与\"" + target + "\"存在一定相关性，但具体匹配点需要进一步验证。";
    }

    private String buildRiskSummary(List<InterviewQuestionEvidenceVO> evidenceList) {
        String projectEvidence = firstEvidenceContaining(evidenceList, "项目经历");
        if (hasText(projectEvidence)) {
            return "当前证据更偏职责和功能描述，如\"" + compact(projectEvidence) + "\"；复杂度、指标结果和个人贡献边界仍需要进一步验证。";
        }
        return "当前证据量有限，项目复杂度、结果指标和个人贡献边界需要进一步验证。";
    }

    private String buildFollowUpSummary(List<InterviewQuestionEvidenceVO> evidenceList) {
        String projectEvidence = firstEvidenceContaining(evidenceList, "项目经历");
        String workEvidence = firstEvidenceContaining(evidenceList, "工作经验");
        if (hasText(projectEvidence) && hasText(workEvidence)) {
            return "建议优先围绕\"" + compact(projectEvidence) + "\"追问项目目标、模块职责和结果衡量，再围绕\"" + compact(workEvidence) + "\"核实技术方案与个人贡献。";
        }
        if (hasText(projectEvidence)) {
            return "建议优先围绕\"" + compact(projectEvidence) + "\"追问项目目标、模块职责、技术方案和结果衡量。";
        }
        if (hasText(workEvidence)) {
            return "建议优先围绕\"" + compact(workEvidence) + "\"核实职责边界、技术取舍和结果产出。";
        }
        return "建议优先从候选人最相关的一段项目或工作经历切入，核实职责边界、技术方案和结果产出。";
    }

    private String firstEvidenceContaining(List<InterviewQuestionEvidenceVO> evidenceList, String section) {
        return evidenceList.stream()
            .filter(evidence -> section.equals(evidence.getSection()))
            .map(InterviewQuestionEvidenceVO::getContent)
            .filter(this::hasText)
            .findFirst()
            .orElse(null);
    }

    private String compact(String text) {
        if (!hasText(text)) {
            return "";
        }
        String compacted = text.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        return compacted.length() <= 60 ? compacted : compacted.substring(0, 60) + "...";
    }

    private Optional<PositionJD> resolvePositionJd(String positionId) {
        if (!hasText(positionId)) {
            return Optional.empty();
        }
        return positionJDRepository.findById(positionId.trim())
            .or(() -> positionJDRepository.findByJdNo(positionId.trim()));
    }

    @SafeVarargs
    private final List<String> mergeKeywords(List<String>... groups) {
        Set<String> merged = new LinkedHashSet<>();
        for (List<String> group : groups) {
            if (group == null) {
                continue;
            }
            for (String value : group) {
                if (hasText(value)) {
                    merged.add(value.trim());
                }
            }
        }
        return new ArrayList<>(merged);
    }

    private List<String> tokenize(String text) {
        if (!hasText(text)) {
            return List.of();
        }
        String normalized = text.replace("、", ",")
            .replace("，", ",")
            .replace("/", " ")
            .replace("|", " ");
        return Arrays.stream(normalized.split("[,\\s]+"))
            .map(String::trim)
            .filter(token -> !token.isBlank())
            .toList();
    }

    private String resolveStrongestSkill(CandidateProfileIndex profile, PositionJD positionJD) {
        List<String> profileSkills = profile.getTechnicalSkills() == null ? List.of() : profile.getTechnicalSkills();
        List<String> jdSkills = mergeKeywords(
            tokenize(positionJD == null ? null : positionJD.getPrioritySkills()),
            tokenize(positionJD == null ? null : positionJD.getBonusSkills())
        );
        for (String jdSkill : jdSkills) {
            for (String profileSkill : profileSkills) {
                if (profileSkill.equalsIgnoreCase(jdSkill)) {
                    return profileSkill;
                }
            }
        }
        return profileSkills.isEmpty() ? null : profileSkills.get(0);
    }

    private String normalizeSkillText(String text) {
        if (!hasText(text)) {
            return null;
        }
        return String.join(" / ", tokenize(text));
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}
