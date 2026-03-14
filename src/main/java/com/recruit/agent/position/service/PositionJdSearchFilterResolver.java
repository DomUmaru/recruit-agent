package com.recruit.agent.position.service;

import com.recruit.agent.candidate.model.CareerStage;
import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.position.model.PositionJD;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Converts persisted JD fields into default search constraints.
 */
@Service
public class PositionJdSearchFilterResolver {

    public CandidateSearchFilter resolve(PositionJD positionJD) {
        CandidateSearchFilter filter = new CandidateSearchFilter();
        if (positionJD == null) {
            return filter;
        }

        filter.setCareerStage(resolveCareerStage(positionJD));
        filter.setHighestDegrees(resolveDegree(positionJD.getRequiredDegree()));
        filter.setMinYearsOfExperience(positionJD.getMinYearsOfExperience() == null
            ? null
            : BigDecimal.valueOf(positionJD.getMinYearsOfExperience()));
        filter.setCurrentCity(normalizeText(positionJD.getLocation()));
        return filter;
    }

    private CareerStage resolveCareerStage(PositionJD positionJD) {
        String text = (normalizeText(positionJD.getTitle()) + " " + normalizeText(positionJD.getRawJdText()))
            .toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return null;
        }
        if (containsAny(text, "校招", "应届", "毕业生", "实习")) {
            return CareerStage.EARLY_CAREER;
        }
        if (containsAny(text, "社招", "资深", "高级", "多年经验")) {
            return CareerStage.EXPERIENCED;
        }
        return null;
    }

    private List<DegreeLevel> resolveDegree(String requiredDegree) {
        String text = normalizeText(requiredDegree).toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return null;
        }
        if (containsAny(text, "博士", "doctor", "phd")) {
            return List.of(DegreeLevel.DOCTOR);
        }
        if (containsAny(text, "硕士", "master")) {
            return List.of(DegreeLevel.MASTER);
        }
        if (containsAny(text, "本科", "学士", "bachelor")) {
            return List.of(DegreeLevel.BACHELOR);
        }
        if (containsAny(text, "大专", "专科", "associate")) {
            return List.of(DegreeLevel.ASSOCIATE);
        }
        return null;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.trim();
    }
}
