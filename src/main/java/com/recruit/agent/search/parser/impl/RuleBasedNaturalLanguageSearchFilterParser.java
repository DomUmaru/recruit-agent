package com.recruit.agent.search.parser.impl;

import com.recruit.agent.candidate.model.CareerStage;
import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import com.recruit.agent.search.dto.CandidateSearchFilter;
import com.recruit.agent.search.parser.NaturalLanguageSearchFilterParser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class RuleBasedNaturalLanguageSearchFilterParser implements NaturalLanguageSearchFilterParser {

    private static final Pattern YEARS_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*(年|years?)");

    private static final List<String> REMOVABLE_TERMS = List.of(
        "博士", "phd", "doctor", "硕士", "研究生", "master", "本科", "学士", "bachelor",
        "大专", "专科", "associate", "高中", "high school",
        "校招", "应届", "学生", "实习", "无经验", "毕业生", "社招", "社招岗", "资深", "多年经验",
        "c9", "985", "project 985", "211", "project 211", "双一流", "double first class",
        "海外名校", "海归名校", "overseas top",
        "上海", "北京", "深圳", "杭州", "广州", "成都", "重庆", "南京", "武汉",
        "大厂", "一线厂", "互联网大厂",
        "外包", "驻场", "不要外包", "排除外包", "非外包", "不是外包", "不要"
    );

    @Override
    public CandidateSearchFilter parse(String text) {
        String normalized = normalize(text);
        CandidateSearchFilter filter = new CandidateSearchFilter();
        filter.setCareerStage(parseCareerStage(normalized));
        filter.setHighestDegrees(parseDegrees(normalized));
        filter.setSchoolTiers(parseSchoolTiers(normalized));
        filter.setMinYearsOfExperience(parseMinYears(normalized));
        filter.setTechnicalSkills(parseTechnicalSkills(normalized));
        filter.setCurrentCity(parseCity(normalized));
        filter.setBigTech(parseBigTech(normalized));
        filter.setOutsourcing(parseOutsourcing(normalized));
        return filter;
    }

    @Override
    public String stripFilterTerms(String text) {
        String cleaned = normalize(text);
        cleaned = YEARS_PATTERN.matcher(cleaned).replaceAll(" ");
        for (String term : REMOVABLE_TERMS) {
            cleaned = cleaned.replace(term.toLowerCase(Locale.ROOT), " ");
        }
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned;
    }

    private CareerStage parseCareerStage(String text) {
        if (containsAny(text, "校招", "应届", "学生", "实习", "无经验", "毕业生")) {
            return CareerStage.EARLY_CAREER;
        }
        if (containsAny(text, "社招", "社招岗", "资深", "多年经验")) {
            return CareerStage.EXPERIENCED;
        }
        return null;
    }

    private List<DegreeLevel> parseDegrees(String text) {
        Set<DegreeLevel> result = new LinkedHashSet<>();
        if (containsAny(text, "博士", "phd", "doctor")) {
            result.add(DegreeLevel.DOCTOR);
        }
        if (containsAny(text, "硕士", "研究生", "master")) {
            result.add(DegreeLevel.MASTER);
        }
        if (containsAny(text, "本科", "学士", "bachelor")) {
            result.add(DegreeLevel.BACHELOR);
        }
        if (containsAny(text, "大专", "专科", "associate")) {
            result.add(DegreeLevel.ASSOCIATE);
        }
        if (containsAny(text, "高中", "high school")) {
            result.add(DegreeLevel.HIGH_SCHOOL);
        }
        return result.isEmpty() ? null : new ArrayList<>(result);
    }

    private List<SchoolTier> parseSchoolTiers(String text) {
        Set<SchoolTier> result = new LinkedHashSet<>();
        if (containsAny(text, "c9")) {
            result.add(SchoolTier.C9);
        }
        if (containsAny(text, "985", "project 985")) {
            result.add(SchoolTier.PROJECT_985);
        }
        if (containsAny(text, "211", "project 211")) {
            result.add(SchoolTier.PROJECT_211);
        }
        if (containsAny(text, "双一流", "double first class")) {
            result.add(SchoolTier.DOUBLE_FIRST_CLASS);
        }
        if (containsAny(text, "海外名校", "海归名校", "overseas top")) {
            result.add(SchoolTier.OVERSEAS_TOP);
        }
        return result.isEmpty() ? null : new ArrayList<>(result);
    }

    private BigDecimal parseMinYears(String text) {
        Matcher matcher = YEARS_PATTERN.matcher(text);
        if (matcher.find()) {
            return new BigDecimal(matcher.group(1));
        }
        return null;
    }

    private List<String> parseTechnicalSkills(String text) {
        Set<String> skills = new LinkedHashSet<>();
        addIfContains(text, skills, "java", "Java");
        addIfContains(text, skills, "spring boot", "Spring Boot");
        addIfContains(text, skills, "spring", "Spring");
        addIfContains(text, skills, "elasticsearch", "Elasticsearch");
        addIfContains(text, skills, "mysql", "MySQL");
        addIfContains(text, skills, "redis", "Redis");
        addIfContains(text, skills, "kafka", "Kafka");
        addIfContains(text, skills, "python", "Python");
        addIfContains(text, skills, "golang", "Go");
        addIfContains(text, skills, "go", "Go");
        addIfContains(text, skills, "c++", "C++");
        addIfContains(text, skills, "docker", "Docker");
        addIfContains(text, skills, "kubernetes", "Kubernetes");
        addIfContains(text, skills, "react", "React");
        addIfContains(text, skills, "vue", "Vue");
        return skills.isEmpty() ? null : new ArrayList<>(skills);
    }

    private String parseCity(String text) {
        if (containsAny(text, "上海")) {
            return "上海";
        }
        if (containsAny(text, "北京")) {
            return "北京";
        }
        if (containsAny(text, "深圳")) {
            return "深圳";
        }
        if (containsAny(text, "杭州")) {
            return "杭州";
        }
        if (containsAny(text, "广州")) {
            return "广州";
        }
        if (containsAny(text, "成都")) {
            return "成都";
        }
        if (containsAny(text, "重庆")) {
            return "重庆";
        }
        if (containsAny(text, "南京")) {
            return "南京";
        }
        if (containsAny(text, "武汉")) {
            return "武汉";
        }
        return null;
    }

    private Boolean parseBigTech(String text) {
        return containsAny(text, "大厂", "一线厂", "互联网大厂") ? Boolean.TRUE : null;
    }

    private Boolean parseOutsourcing(String text) {
        if (containsAny(text, "不要外包", "排除外包", "非外包", "不是外包")) {
            return false;
        }
        if (containsAny(text, "外包", "驻场")) {
            return true;
        }
        return null;
    }

    private void addIfContains(String text, Set<String> values, String keyword, String normalizedValue) {
        if (text.contains(keyword)) {
            values.add(normalizedValue);
        }
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }
}
