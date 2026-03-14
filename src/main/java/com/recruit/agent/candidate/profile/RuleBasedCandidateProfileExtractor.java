package com.recruit.agent.candidate.profile;

import com.recruit.agent.candidate.model.CareerStage;
import com.recruit.agent.candidate.model.DegreeLevel;
import com.recruit.agent.candidate.model.SchoolTier;
import com.recruit.agent.resume.model.ResumeDocument;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 基于规则的候选人画像抽取实现。
 */
@Component
public class RuleBasedCandidateProfileExtractor implements CandidateProfileExtractor {

    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{1,2})(?:\\+)?\\s*(?:年|years?|yrs?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCHOOL_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z]{2,30}(大学|学院|University|College))");

    private static final List<String> TECH_KEYWORDS = List.of(
        "Java", "Spring", "Spring Boot", "Spring Cloud", "MySQL", "Redis", "Kafka",
        "Elasticsearch", "Docker", "Kubernetes", "Python", "Go", "Linux", "Vue", "React"
    );

    private static final List<String> BIG_TECH_KEYWORDS = List.of(
        "阿里", "腾讯", "字节", "百度", "美团", "京东", "华为", "滴滴", "小米", "快手", "网易", "拼多多"
    );

    private static final List<String> OUTSOURCING_KEYWORDS = List.of("外包", "驻场", "外派");

    @Override
    public CandidateProfileDraft extract(ResumeDocument document) {
        String text = safeText(document.getCleanedText());

        CandidateProfileDraft draft = new CandidateProfileDraft();
        draft.setCareerStage(extractCareerStage(text));
        draft.setHighestDegree(extractDegree(text));
        draft.setSchoolName(extractSchool(text));
        draft.setSchoolTier(extractSchoolTier(text));
        draft.setTotalYearsOfExperience(extractYears(text));
        draft.setTechnicalSkills(extractTechSkills(text));
        draft.setIndustryTags(List.of());
        draft.setCompanyTags(extractCompanyTags(text));
        draft.setProjectTags(extractProjectTags(text));
        draft.setBigTech(containsAny(text, BIG_TECH_KEYWORDS));
        draft.setOutsourcing(containsAny(text, OUTSOURCING_KEYWORDS));
        draft.setProfileSummary(buildSummary(text));
        draft.setMetadata(Map.of(
            "extractor", "rule-based",
            "resumeDocumentId", document.getId(),
            "parseType", document.getParseType() == null ? "UNKNOWN" : document.getParseType().name()
        ));
        return draft;
    }

    private CareerStage extractCareerStage(String text) {
        if (containsAny(text, List.of("校招", "应届", "毕业生", "学生", "实习"))) {
            return CareerStage.EARLY_CAREER;
        }
        BigDecimal years = extractYears(text);
        if (years != null && years.compareTo(BigDecimal.ONE) <= 0) {
            return CareerStage.EARLY_CAREER;
        }
        if (years != null && years.compareTo(new BigDecimal("2")) >= 0) {
            return CareerStage.EXPERIENCED;
        }
        return null;
    }

    private DegreeLevel extractDegree(String text) {
        if (text.contains("博士")) {
            return DegreeLevel.DOCTOR;
        }
        if (containsIgnoreCase(text, "doctor") || containsIgnoreCase(text, "phd")) {
            return DegreeLevel.DOCTOR;
        }
        if (text.contains("硕士") || text.contains("研究生")) {
            return DegreeLevel.MASTER;
        }
        if (containsIgnoreCase(text, "master")) {
            return DegreeLevel.MASTER;
        }
        if (text.contains("本科") || text.contains("学士")) {
            return DegreeLevel.BACHELOR;
        }
        if (containsIgnoreCase(text, "bachelor")) {
            return DegreeLevel.BACHELOR;
        }
        if (text.contains("大专")) {
            return DegreeLevel.ASSOCIATE;
        }
        if (containsIgnoreCase(text, "associate")) {
            return DegreeLevel.ASSOCIATE;
        }
        if (text.contains("高中")) {
            return DegreeLevel.HIGH_SCHOOL;
        }
        if (containsIgnoreCase(text, "high school")) {
            return DegreeLevel.HIGH_SCHOOL;
        }
        return null;
    }

    private String extractSchool(String text) {
        Matcher matcher = SCHOOL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private SchoolTier extractSchoolTier(String text) {
        if (text.contains("985")) {
            return SchoolTier.PROJECT_985;
        }
        if (text.contains("211")) {
            return SchoolTier.PROJECT_211;
        }
        if (text.contains("双一流")) {
            return SchoolTier.DOUBLE_FIRST_CLASS;
        }
        return null;
    }

    private BigDecimal extractYears(String text) {
        Matcher matcher = YEAR_PATTERN.matcher(text);
        int maxYears = 0;
        while (matcher.find()) {
            maxYears = Math.max(maxYears, Integer.parseInt(matcher.group(1)));
        }
        if (maxYears == 0) {
            return null;
        }
        return BigDecimal.valueOf(maxYears).setScale(1, RoundingMode.HALF_UP);
    }

    private List<String> extractTechSkills(String text) {
        Set<String> result = new LinkedHashSet<>();
        String normalized = text.toLowerCase();
        for (String keyword : TECH_KEYWORDS) {
            if (normalized.contains(keyword.toLowerCase())) {
                result.add(keyword);
            }
        }
        return new ArrayList<>(result);
    }

    private List<String> extractCompanyTags(String text) {
        Set<String> result = new LinkedHashSet<>();
        for (String keyword : BIG_TECH_KEYWORDS) {
            if (text.contains(keyword)) {
                result.add(keyword);
            }
        }
        return new ArrayList<>(result);
    }

    private List<String> extractProjectTags(String text) {
        Set<String> result = new LinkedHashSet<>();
        if (text.contains("推荐")) {
            result.add("推荐系统");
        }
        if (text.contains("搜索")) {
            result.add("搜索");
        }
        if (text.contains("广告")) {
            result.add("广告");
        }
        if (text.contains("风控")) {
            result.add("风控");
        }
        if (text.contains("中台")) {
            result.add("中台");
        }
        return new ArrayList<>(result);
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return text != null && text.toLowerCase().contains(keyword.toLowerCase());
    }

    private String buildSummary(String text) {
        String normalized = text.replace('\n', ' ').trim();
        if (normalized.length() <= 300) {
            return normalized;
        }
        return normalized.substring(0, 300);
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }
}
