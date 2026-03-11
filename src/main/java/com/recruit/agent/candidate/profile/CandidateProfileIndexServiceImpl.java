package com.recruit.agent.candidate.profile;

import com.recruit.agent.candidate.model.Candidate;
import com.recruit.agent.candidate.repository.CandidateRepository;
import com.recruit.agent.rag.model.CandidateProfileIndex;
import com.recruit.agent.rag.repository.CandidateProfileIndexRepository;
import com.recruit.agent.resume.model.ResumeDocument;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 候选人画像索引服务实现。
 */
@Service
public class CandidateProfileIndexServiceImpl implements CandidateProfileIndexService {

    private final CandidateProfileExtractor candidateProfileExtractor;
    private final CandidateRepository candidateRepository;
    private final CandidateProfileIndexRepository candidateProfileIndexRepository;

    public CandidateProfileIndexServiceImpl(CandidateProfileExtractor candidateProfileExtractor,
                                            CandidateRepository candidateRepository,
                                            CandidateProfileIndexRepository candidateProfileIndexRepository) {
        this.candidateProfileExtractor = candidateProfileExtractor;
        this.candidateRepository = candidateRepository;
        this.candidateProfileIndexRepository = candidateProfileIndexRepository;
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
}
