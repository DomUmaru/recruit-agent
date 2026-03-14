package com.recruit.agent.candidate.school;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DefaultSchoolTierResolver implements SchoolTierResolver {

    private final Set<String> c9;
    private final Set<String> project985;
    private final Set<String> project211;
    private final Set<String> doubleFirstClass;
    private final Set<String> overseasTop;

    public DefaultSchoolTierResolver(ObjectMapper objectMapper) {
        SchoolTierMapping mapping = loadMapping(objectMapper);
        this.c9 = normalizeSet(mapping.getC9());
        this.project985 = normalizeSet(mapping.getProject985());
        this.project211 = normalizeSet(mapping.getProject211());
        this.doubleFirstClass = normalizeSet(mapping.getDoubleFirstClass());
        this.overseasTop = normalizeSet(mapping.getOverseasTop());
    }

    @Override
    public SchoolTier resolve(String schoolName, DegreeLevel degreeLevel) {
        if (!StringUtils.hasText(schoolName)) {
            return fallbackFromDegree(degreeLevel);
        }

        String normalized = normalizeSchoolName(schoolName);
        if (normalized.isBlank()) {
            return fallbackFromDegree(degreeLevel);
        }
        if (c9.contains(normalized)) {
            return SchoolTier.C9;
        }
        if (project985.contains(normalized)) {
            return SchoolTier.PROJECT_985;
        }
        if (project211.contains(normalized)) {
            return SchoolTier.PROJECT_211;
        }
        if (doubleFirstClass.contains(normalized)) {
            return SchoolTier.DOUBLE_FIRST_CLASS;
        }
        if (overseasTop.contains(normalized)) {
            return SchoolTier.OVERSEAS_TOP;
        }
        if (isJuniorCollege(normalized, degreeLevel)) {
            return SchoolTier.JUNIOR_COLLEGE;
        }
        if (isGeneralUndergrad(normalized, degreeLevel)) {
            return SchoolTier.GENERAL_UNDERGRAD;
        }
        return SchoolTier.OTHER;
    }

    private SchoolTier fallbackFromDegree(DegreeLevel degreeLevel) {
        if (degreeLevel == null) {
            return null;
        }
        if (degreeLevel == DegreeLevel.ASSOCIATE || degreeLevel == DegreeLevel.HIGH_SCHOOL) {
            return SchoolTier.JUNIOR_COLLEGE;
        }
        if (degreeLevel == DegreeLevel.BACHELOR
            || degreeLevel == DegreeLevel.MASTER
            || degreeLevel == DegreeLevel.DOCTOR) {
            return SchoolTier.GENERAL_UNDERGRAD;
        }
        return SchoolTier.OTHER;
    }

    private boolean isJuniorCollege(String normalizedSchoolName, DegreeLevel degreeLevel) {
        if (degreeLevel == DegreeLevel.ASSOCIATE || degreeLevel == DegreeLevel.HIGH_SCHOOL) {
            return true;
        }
        return normalizedSchoolName.contains("职业技术学院")
            || normalizedSchoolName.contains("职业学院")
            || normalizedSchoolName.contains("高等专科学校")
            || normalizedSchoolName.contains("专科学校");
    }

    private boolean isGeneralUndergrad(String normalizedSchoolName, DegreeLevel degreeLevel) {
        if (degreeLevel == DegreeLevel.BACHELOR
            || degreeLevel == DegreeLevel.MASTER
            || degreeLevel == DegreeLevel.DOCTOR) {
            return true;
        }
        return normalizedSchoolName.contains("大学")
            || (normalizedSchoolName.contains("学院") && !normalizedSchoolName.contains("职业学院"));
    }

    private SchoolTierMapping loadMapping(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource("school-tier-mapping.json");
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, SchoolTierMapping.class);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load school tier mapping", ex);
        }
    }

    private Set<String> normalizeSet(List<String> values) {
        Set<String> result = new HashSet<>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                result.add(normalizeSchoolName(value));
            }
        }
        return result;
    }

    private String normalizeSchoolName(String schoolName) {
        return schoolName
            .replace('\u00A0', ' ')
            .replace("（", "(")
            .replace("）", ")")
            .replaceAll("\\(.*?\\)", "")
            .replaceAll("\\s+", "")
            .trim()
            .toLowerCase(Locale.ROOT);
    }
}
