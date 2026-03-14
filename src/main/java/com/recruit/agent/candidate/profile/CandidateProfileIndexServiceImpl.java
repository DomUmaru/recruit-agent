package com.recruit.agent.candidate.profile;

import com.recruit.agent.candidate.model.Candidate;
import com.recruit.agent.candidate.repository.CandidateRepository;
import com.recruit.agent.rag.embedding.EmbeddingService;
import com.recruit.agent.rag.model.CandidateProfileIndex;
import com.recruit.agent.rag.repository.CandidateProfileIndexRepository;
import com.recruit.agent.resume.model.ResumeDocument;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 候选人画像索引服务实现。
 */
@Service
public class CandidateProfileIndexServiceImpl implements CandidateProfileIndexService {

    private static final Logger log = LoggerFactory.getLogger(CandidateProfileIndexServiceImpl.class);

    private final CandidateProfileExtractor candidateProfileExtractor;
    private final CandidateRepository candidateRepository;
    private final CandidateProfileIndexRepository candidateProfileIndexRepository;
    private final EmbeddingService embeddingService;

    public CandidateProfileIndexServiceImpl(CandidateProfileExtractor candidateProfileExtractor,
                                            CandidateRepository candidateRepository,
                                            CandidateProfileIndexRepository candidateProfileIndexRepository,
                                            EmbeddingService embeddingService) {
        this.candidateProfileExtractor = candidateProfileExtractor;
        this.candidateRepository = candidateRepository;
        this.candidateProfileIndexRepository = candidateProfileIndexRepository;
        this.embeddingService = embeddingService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void indexProfile(ResumeDocument document) {
        Candidate candidate = document.getCandidate();
        CandidateProfileDraft draft = candidateProfileExtractor.extract(document);

        mergeIntoCandidate(candidate, draft);
        candidateRepository.save(candidate);

        CandidateProfileIndex profileIndex = candidateProfileIndexRepository.findByCandidateId(candidate.getId())
            .orElseGet(CandidateProfileIndex::new);

        if (profileIndex.getId() == null || profileIndex.getId().isBlank()) {
            profileIndex.setId(UUID.randomUUID().toString());
        }

        profileIndex.setCandidateId(candidate.getId());
        profileIndex.setCandidateNo(candidate.getCandidateNo());
        profileIndex.setCareerStage(draft.getCareerStage() == null ? null : draft.getCareerStage().name());
        profileIndex.setFullName(candidate.getFullName());
        profileIndex.setCurrentCity(candidate.getCurrentCity());
        profileIndex.setSchoolName(draft.getSchoolName());
        profileIndex.setHighestDegree(draft.getHighestDegree() == null ? null : draft.getHighestDegree().name());
        profileIndex.setSchoolTier(draft.getSchoolTier() == null ? null : draft.getSchoolTier().name());
        profileIndex.setTotalYearsOfExperience(draft.getTotalYearsOfExperience());
        profileIndex.setTechnicalSkills(draft.getTechnicalSkills());
        profileIndex.setIndustryTags(draft.getIndustryTags());
        profileIndex.setCompanyTags(draft.getCompanyTags());
        profileIndex.setProjectTags(draft.getProjectTags());
        profileIndex.setBigTech(draft.getBigTech());
        profileIndex.setOutsourcing(draft.getOutsourcing());
        profileIndex.setProfileSummary(draft.getProfileSummary());
        profileIndex.setMetadata(draft.getMetadata());
        applyEmbedding(profileIndex);
        profileIndex.setIndexedAt(LocalDateTime.now());

        candidateProfileIndexRepository.save(profileIndex);
    }

    private void mergeIntoCandidate(Candidate candidate, CandidateProfileDraft draft) {
        if (draft.getHighestDegree() != null) {
            candidate.setHighestDegree(draft.getHighestDegree());
        }
        if (draft.getSchoolName() != null && !draft.getSchoolName().isBlank()) {
            candidate.setSchoolName(draft.getSchoolName());
        }
        if (draft.getSchoolTier() != null) {
            candidate.setSchoolTier(draft.getSchoolTier());
        }
        if (draft.getTotalYearsOfExperience() != null) {
            candidate.setTotalYearsOfExperience(draft.getTotalYearsOfExperience());
        }
        if (draft.getBigTech() != null) {
            candidate.setBigTech(draft.getBigTech());
        }
        if (draft.getOutsourcing() != null) {
            candidate.setOutsourcing(draft.getOutsourcing());
        }
        if (draft.getProfileSummary() != null && !draft.getProfileSummary().isBlank()) {
            candidate.setSummary(draft.getProfileSummary());
        }
    }

    private void applyEmbedding(CandidateProfileIndex profileIndex) {
        if (!embeddingService.isAvailable()) {
            return;
        }
        String embeddingText = buildEmbeddingText(profileIndex);
        if (embeddingText.isBlank()) {
            return;
        }
        try {
            List<float[]> embeddings = embeddingService.embedAll(List.of(embeddingText));
            if (embeddings == null || embeddings.isEmpty() || embeddings.get(0) == null || embeddings.get(0).length == 0) {
                return;
            }
            profileIndex.setEmbedding(embeddings.get(0));
        } catch (IOException exception) {
            log.warn("Failed to generate candidate profile embedding. Fallback to keyword-only profile index.", exception);
        }
    }

    private String buildEmbeddingText(CandidateProfileIndex profileIndex) {
        List<String> parts = new ArrayList<>();
        add(parts, profileIndex.getFullName());
        add(parts, profileIndex.getCurrentCity());
        add(parts, profileIndex.getCareerStage());
        add(parts, profileIndex.getSchoolName());
        add(parts, profileIndex.getHighestDegree());
        add(parts, profileIndex.getSchoolTier());
        add(parts, profileIndex.getProfileSummary());
        add(parts, join(profileIndex.getTechnicalSkills()));
        add(parts, join(profileIndex.getIndustryTags()));
        add(parts, join(profileIndex.getCompanyTags()));
        add(parts, join(profileIndex.getProjectTags()));
        return String.join("\n", parts).toLowerCase(Locale.ROOT).trim();
    }

    private void add(List<String> parts, String text) {
        if (text != null && !text.isBlank()) {
            parts.add(text);
        }
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(", ", values);
    }
}
