package com.recruit.agent.interview.service.impl;

import com.recruit.agent.interview.dto.InterviewQuestionRequest;
import com.recruit.agent.interview.service.InterviewQuestionService;
import com.recruit.agent.interview.vo.CandidateInterviewQuestionVO;
import com.recruit.agent.interview.vo.InterviewQuestionEvidenceVO;
import com.recruit.agent.interview.vo.InterviewQuestionItemVO;
import com.recruit.agent.interview.vo.InterviewQuestionResponse;
import com.recruit.agent.llm.LlmGenerationService;
import com.recruit.agent.rag.model.CandidateProfileIndex;
import com.recruit.agent.rag.model.ResumeChunk;
import com.recruit.agent.rag.repository.CandidateProfileIndexRepository;
import com.recruit.agent.rag.repository.ResumeChunkRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 面试题生成服务实现。
 */
@Service
public class InterviewQuestionServiceImpl implements InterviewQuestionService {

    private static final Logger log = LoggerFactory.getLogger(InterviewQuestionServiceImpl.class);

    private final CandidateProfileIndexRepository candidateProfileIndexRepository;
    private final ResumeChunkRepository resumeChunkRepository;
    private final LlmGenerationService llmGenerationService;

    public InterviewQuestionServiceImpl(CandidateProfileIndexRepository candidateProfileIndexRepository,
                                        ResumeChunkRepository resumeChunkRepository,
                                        LlmGenerationService llmGenerationService) {
        this.candidateProfileIndexRepository = candidateProfileIndexRepository;
        this.resumeChunkRepository = resumeChunkRepository;
        this.llmGenerationService = llmGenerationService;
    }

    @Override
    public InterviewQuestionResponse generate(InterviewQuestionRequest request) {
        InterviewQuestionRequest safeRequest = request == null ? new InterviewQuestionRequest() : request;
        List<CandidateInterviewQuestionVO> candidates = safeRequest.getCandidateIds() == null
            ? List.of()
            : safeRequest.getCandidateIds().stream()
                .map(candidateId -> buildCandidateQuestions(candidateId, safeRequest.getTargetQuery()))
                .filter(Objects::nonNull)
                .toList();

        InterviewQuestionResponse response = new InterviewQuestionResponse();
        response.setTargetQuery(safeRequest.getTargetQuery());
        response.setCandidates(candidates);
        response.setSummary(buildSummaryWithLlm(candidates, safeRequest.getTargetQuery()));
        return response;
    }

    private CandidateInterviewQuestionVO buildCandidateQuestions(String candidateId, String targetQuery) {
        CandidateProfileIndex profile = candidateProfileIndexRepository.findByCandidateId(candidateId).orElse(null);
        if (profile == null) {
            return null;
        }

        List<ResumeChunk> evidenceChunks = resumeChunkRepository.findByCandidateId(candidateId).stream()
            .sorted(Comparator.comparing(ResumeChunk::getChunkOrder, Comparator.nullsLast(Integer::compareTo)))
            .limit(3)
            .toList();

        CandidateInterviewQuestionVO candidate = new CandidateInterviewQuestionVO();
        candidate.setCandidateId(profile.getCandidateId());
        candidate.setCandidateNo(profile.getCandidateNo());
        candidate.setFullName(profile.getFullName());
        candidate.setQuestions(buildQuestions(profile, evidenceChunks, targetQuery));
        return candidate;
    }

    private List<InterviewQuestionItemVO> buildQuestions(CandidateProfileIndex profile,
                                                         List<ResumeChunk> evidenceChunks,
                                                         String targetQuery) {
        List<InterviewQuestionItemVO> questions = new ArrayList<>();

        questions.add(buildQuestion(
            "项目经历",
            "请结合你在简历中最有代表性的项目，说明你负责的核心模块、技术决策和最终业务结果。",
            "验证候选人是否真正主导过项目，并核对业务影响。",
            evidenceChunks
        ));

        questions.add(buildQuestion(
            "需求匹配",
            buildRequirementFitQuestion(profile, targetQuery),
            "核验候选人与当前岗位需求的贴合度。",
            evidenceChunks
        ));

        if (profile.getTechnicalSkills() != null && !profile.getTechnicalSkills().isEmpty()) {
            questions.add(buildQuestion(
                "技术深挖",
                "你在 " + profile.getTechnicalSkills().get(0) + " 上最复杂的一次实践是什么？当时遇到的性能、稳定性或可维护性问题是如何解决的？",
                "围绕候选人简历中出现的核心技术做深挖。",
                evidenceChunks
            ));
        }

        if (Boolean.TRUE.equals(profile.getOutsourcing())) {
            questions.add(buildQuestion(
                "履历风险",
                "简历中出现了外包/驻场经历，请具体说明你的直接汇报关系、承担职责，以及你在项目中的实际决策权边界。",
                "确认候选人是否承担核心职责，降低履历包装风险。",
                evidenceChunks
            ));
        }

        if (profile.getTotalYearsOfExperience() != null) {
            questions.add(buildQuestion(
                "经验复盘",
                buildExperienceQuestion(profile.getTotalYearsOfExperience()),
                "核验年限与能力深度是否匹配。",
                evidenceChunks
            ));
        }

        return questions.stream().limit(5).toList();
    }

    private String buildRequirementFitQuestion(CandidateProfileIndex profile, String targetQuery) {
        if (targetQuery == null || targetQuery.isBlank()) {
            return "结合你的履历，哪一段经历最能说明你适合当前应聘岗位？请说明你的直接贡献和产出。";
        }
        return "针对\"" + targetQuery + "\"这类需求，你过去最相关的一段经历是什么？请按背景、方案、结果展开说明。";
    }

    private String buildExperienceQuestion(BigDecimal years) {
        if (years.compareTo(new BigDecimal("5")) >= 0) {
            return "你有 " + years + " 年左右经验，请举例说明你如何做技术取舍、推动协作并对结果负责。";
        }
        return "你目前有 " + years + " 年左右经验，请举一个你成长最快的项目，说明你承担了哪些超出预期的职责。";
    }

    private InterviewQuestionItemVO buildQuestion(String category,
                                                  String question,
                                                  String rationale,
                                                  List<ResumeChunk> evidenceChunks) {
        InterviewQuestionItemVO item = new InterviewQuestionItemVO();
        item.setCategory(category);
        item.setQuestion(question);
        item.setRationale(rationale);
        item.setEvidenceList(evidenceChunks.stream()
            .map(this::toEvidence)
            .toList());
        return item;
    }

    private InterviewQuestionEvidenceVO toEvidence(ResumeChunk chunk) {
        InterviewQuestionEvidenceVO evidence = new InterviewQuestionEvidenceVO();
        evidence.setSection(chunk.getSection());
        evidence.setPage(chunk.getPage());
        evidence.setContent(chunk.getContent());
        return evidence;
    }

    private String buildSummary(List<CandidateInterviewQuestionVO> candidates, String targetQuery) {
        if (candidates.isEmpty()) {
            return "未找到可生成面试题的候选人。";
        }
        String queryPart = targetQuery == null || targetQuery.isBlank()
            ? "已生成候选人面试题"
            : "已围绕\"" + targetQuery + "\"生成候选人面试题";
        return queryPart + "，可优先从项目经历、技术深挖和履历风险三个方向展开追问。";
    }

    private String buildSummaryWithLlm(List<CandidateInterviewQuestionVO> candidates, String targetQuery) {
        String fallback = buildSummary(candidates, targetQuery);
        if (candidates.isEmpty()) {
            return fallback;
        }
        if (!llmGenerationService.isAvailable()) {
            log.info("Interview summary uses deterministic fallback because LLM service is unavailable.");
            return fallback;
        }
        try {
            String summary = llmGenerationService.generate(
                buildInterviewSystemPrompt(),
                buildInterviewUserPrompt(candidates, targetQuery)
            );
            log.info("Interview summary generated by LLM.");
            return summary == null || summary.isBlank() ? fallback : summary.trim();
        } catch (RuntimeException exception) {
            log.warn("Failed to generate interview summary with LLM. Fallback to rule summary.", exception);
            return fallback;
        }
    }

    private String buildInterviewSystemPrompt() {
        return """
            你是招聘场景的面试准备助手。
            你的任务是基于候选人的面试题清单，生成一段简洁、专业的中文总结，帮助面试官快速把握面试重点。
            要求：
            1. 只根据输入中的目标需求、候选人和题目类别总结。
            2. 不要编造简历中不存在的经历或问题。
            3. 总结控制在 80 到 140 字之间。
            4. 优先指出最值得追问的方向。
            """;
    }

    private String buildInterviewUserPrompt(List<CandidateInterviewQuestionVO> candidates, String targetQuery) {
        StringBuilder builder = new StringBuilder();
        builder.append("目标需求: ").append(targetQuery == null || targetQuery.isBlank() ? "未提供" : targetQuery).append('\n');
        for (CandidateInterviewQuestionVO candidate : candidates) {
            builder.append("候选人: ").append(candidate.getFullName()).append('\n');
            builder.append("题目类别: ")
                .append(candidate.getQuestions().stream().map(InterviewQuestionItemVO::getCategory).distinct().toList())
                .append("\n\n");
        }
        return builder.toString().trim();
    }
}
