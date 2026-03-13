package com.recruit.agent.rag.chunk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.resume.model.ResumeDocument;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class RuleBasedResumeSplitter implements ResumeChunkingService {

    private static final String DEFAULT_SECTION = "通用信息";
    private static final String SECTION_EDUCATION = "教育经历";
    private static final String SECTION_WORK = "工作经验";
    private static final String SECTION_INTERNSHIP = "实习经历";
    private static final String SECTION_PROJECT = "项目经历";
    private static final String SECTION_SKILLS = "专业技能";
    private static final String SECTION_AWARDS = "获奖经历";
    private static final String SECTION_CAMPUS = "校园经历";
    private static final String SECTION_PROFILE = "个人评价";
    private static final String DEFAULT_SUB_SECTION = "未命名子项";

    private static final Map<String, String> SECTION_ALIASES = Map.ofEntries(
        Map.entry("教育经历", SECTION_EDUCATION),
        Map.entry("教育背景", SECTION_EDUCATION),
        Map.entry("教育", SECTION_EDUCATION),
        Map.entry("工作经验", SECTION_WORK),
        Map.entry("工作经历", SECTION_WORK),
        Map.entry("工作履历", SECTION_WORK),
        Map.entry("实习经历", SECTION_INTERNSHIP),
        Map.entry("实习经验", SECTION_INTERNSHIP),
        Map.entry("项目经历", SECTION_PROJECT),
        Map.entry("项目经验", SECTION_PROJECT),
        Map.entry("项目实践", SECTION_PROJECT),
        Map.entry("专业技能", SECTION_SKILLS),
        Map.entry("技能清单", SECTION_SKILLS),
        Map.entry("技能特长", SECTION_SKILLS),
        Map.entry("技术栈", SECTION_SKILLS),
        Map.entry("获奖经历", SECTION_AWARDS),
        Map.entry("获奖情况", SECTION_AWARDS),
        Map.entry("技术竞赛与奖项", SECTION_AWARDS),
        Map.entry("竞赛奖项", SECTION_AWARDS),
        Map.entry("校园经历", SECTION_CAMPUS),
        Map.entry("校园实践", SECTION_CAMPUS),
        Map.entry("个人评价", SECTION_PROFILE),
        Map.entry("自我评价", SECTION_PROFILE),
        Map.entry("个人优势", SECTION_PROFILE)
    );

    private static final Set<String> SUB_SECTION_SECTIONS = Set.of(
        SECTION_WORK,
        SECTION_INTERNSHIP,
        SECTION_PROJECT
    );

    private static final Pattern BULLET_PREFIX = Pattern.compile(
        "^(?:[-•·●▪■□◆◇▶►*]|\\d+[.)、]|[(（]\\d+[)）]|[一二三四五六七八九十]+[、.)]|[①②③④⑤⑥⑦⑧⑨⑩]).*"
    );
    private static final Pattern TIME_RANGE = Pattern.compile(
        "^(?:\\d{4}[./-](?:\\d{1,2}|至今|现在).*)|(?:\\d{4}年\\d{1,2}月.*)|(?:\\d{4}[./-]\\d{1,2}\\s*[-~至]\\s*(?:\\d{4}[./-]\\d{1,2}|至今|现在).*)$"
    );
    private static final Pattern CONTACT_OR_LINK = Pattern.compile(
        ".*(?:@|电话|手机|微信|github|gitee|邮箱|linkedin|求职意向).*",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NOISE_LABEL = Pattern.compile(
        "^(?:职责|负责|项目描述|技术栈|成果|亮点|工作内容|主要工作|核心职责|背景|难点|效果|业绩)[:：]?$"
    );
    private static final Pattern DETAIL_START = Pattern.compile(
        "^(?:负责|参与|使用|基于|通过|实现|设计|优化|搭建|完成|编写|维护|熟悉|掌握|了解|主导|协助|推动|支持|成果|亮点|项目描述|技术栈|工作内容|职责)[:：]?.*"
    );
    private static final Pattern ENDS_WITH_SENTENCE = Pattern.compile(".*[。；;]$");
    private static final Pattern LEADING_NOISE = Pattern.compile("^[\\p{Punct}\\p{So}\\u3000-\\u303F\\uF000-\\uF8FF\\s]+");
    private static final Pattern TRAILING_TITLE_PUNCT = Pattern.compile("[:：|丨]+$");

    private final ObjectMapper objectMapper;

    public RuleBasedResumeSplitter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ResumeChunkingResult chunk(ResumeDocument document) {
        List<IndexedLine> indexedLines = extractIndexedLines(document);
        List<ParentBlock> parentBlocks = splitParentBlocks(document, indexedLines);
        List<ResumeChunkDraft> chunks = buildDrafts(document, parentBlocks);

        ResumeChunkingResult result = new ResumeChunkingResult();
        result.setChunks(chunks);
        result.setLog("已完成规则化结构切分，生成 " + chunks.size() + " 个分块");
        return result;
    }

    private List<IndexedLine> extractIndexedLines(ResumeDocument document) {
        List<IndexedLine> lines = new ArrayList<>();
        List<String> pages = parsePages(document);
        int pageNo = 1;
        for (String page : pages) {
            for (String rawLine : page.split("\\r?\\n")) {
                for (String fragment : splitLineByEmbeddedSection(rawLine)) {
                    String line = normalizeLine(fragment);
                    if (!line.isBlank()) {
                        lines.add(new IndexedLine(line, pageNo));
                    }
                }
            }
            pageNo++;
        }
        return lines;
    }

    private List<String> parsePages(ResumeDocument document) {
        String pageTextJson = document.getPageTextJson();
        if (pageTextJson != null && !pageTextJson.isBlank()) {
            try {
                List<String> pages = objectMapper.readValue(pageTextJson, new TypeReference<List<String>>() {
                });
                if (pages != null && !pages.isEmpty()) {
                    return pages;
                }
            } catch (Exception ignored) {
                // Fallback to cleaned/raw text below.
            }
        }
        if (document.getCleanedText() != null && !document.getCleanedText().isBlank()) {
            return List.of(document.getCleanedText());
        }
        if (document.getRawText() != null && !document.getRawText().isBlank()) {
            return List.of(document.getRawText());
        }
        return List.of();
    }

    private List<ParentBlock> splitParentBlocks(ResumeDocument document, List<IndexedLine> indexedLines) {
        List<ParentBlock> parents = new ArrayList<>();
        String currentSection = DEFAULT_SECTION;
        ParentBlock currentParent = null;

        for (IndexedLine indexedLine : indexedLines) {
            String line = indexedLine.text();

            String section = detectSection(line);
            if (section != null) {
                currentSection = section;
                currentParent = null;
                continue;
            }

            if (SUB_SECTION_SECTIONS.contains(currentSection)) {
                if (currentParent == null) {
                    String initialSubSectionTitle = isInitialSubSectionTitle(line) ? line : null;
                    currentParent = new ParentBlock(currentSection, initialSubSectionTitle, indexedLine.page());
                    parents.add(currentParent);
                    if (initialSubSectionTitle == null) {
                        currentParent.addLine(line);
                    }
                    continue;
                }
                if (isSubSectionTitle(line, currentParent)) {
                    currentParent = new ParentBlock(currentSection, line, indexedLine.page());
                    parents.add(currentParent);
                    continue;
                }
            } else if (currentParent == null || !currentSection.equals(currentParent.section())) {
                currentParent = new ParentBlock(currentSection, null, indexedLine.page());
                parents.add(currentParent);
            }

            currentParent.addLine(line);
        }

        return parents;
    }

    private List<ResumeChunkDraft> buildDrafts(ResumeDocument document, List<ParentBlock> parentBlocks) {
        List<ResumeChunkDraft> chunks = new ArrayList<>();
        int chunkOrder = 0;

        for (ParentBlock parentBlock : parentBlocks) {
            if (parentBlock.lines().isEmpty() && parentBlock.subSectionTitle() == null) {
                continue;
            }

            String parentId = UUID.randomUUID().toString();
            ResumeChunkDraft parentDraft = new ResumeChunkDraft();
            parentDraft.setParentId(parentId);
            parentDraft.setChunkType(ChunkType.PARENT);
            parentDraft.setSection(parentBlock.section());
            parentDraft.setSubSectionTitle(parentBlock.subSectionTitle());
            parentDraft.setPage(parentBlock.page());
            parentDraft.setChunkOrder(chunkOrder++);
            parentDraft.setContent(parentBlock.parentContent());
            parentDraft.setNormalizedContent(parentBlock.parentContent());
            parentDraft.setTags(buildTags(parentBlock.section(), parentBlock.subSectionTitle(), false));
            parentDraft.setMetadata(buildMetadata(document, parentBlock.page(), ChunkType.PARENT,
                parentBlock.section(), parentBlock.subSectionTitle()));
            chunks.add(parentDraft);

            int childOrder = 0;
            for (String childLine : splitChildLines(parentBlock.lines())) {
                ResumeChunkDraft childDraft = new ResumeChunkDraft();
                childDraft.setParentId(parentId);
                childDraft.setChunkType(ChunkType.CHILD);
                childDraft.setSection(parentBlock.section());
                childDraft.setSubSectionTitle(parentBlock.subSectionTitle());
                childDraft.setPage(parentBlock.page());
                childDraft.setChunkOrder(chunkOrder++);
                childDraft.setContent(childLine);
                childDraft.setNormalizedContent(enrichContent(parentBlock.section(), parentBlock.subSectionTitle(), childLine));
                childDraft.setTags(buildTags(parentBlock.section(), parentBlock.subSectionTitle(), true));
                Map<String, Object> metadata = buildMetadata(document, parentBlock.page(), ChunkType.CHILD,
                    parentBlock.section(), parentBlock.subSectionTitle());
                metadata.put("childOrder", childOrder++);
                childDraft.setMetadata(metadata);
                chunks.add(childDraft);
            }
        }

        return chunks;
    }

    private List<String> splitChildLines(List<String> lines) {
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }

            if (current.isEmpty()) {
                current.append(line);
                continue;
            }

            if (startsNewItem(line, current.toString())) {
                items.add(current.toString().trim());
                current.setLength(0);
                current.append(line);
                continue;
            }

            current.append(" ").append(line);
        }

        if (!current.isEmpty()) {
            items.add(current.toString().trim());
        }

        if (items.isEmpty() && !lines.isEmpty()) {
            items.add(String.join(" ", lines));
        }
        return items;
    }

    private boolean startsNewItem(String line, String currentItem) {
        if (BULLET_PREFIX.matcher(line).matches()) {
            return true;
        }
        if (currentItem.endsWith("：") || currentItem.endsWith(":")) {
            return false;
        }
        if (DETAIL_START.matcher(line).matches()) {
            return true;
        }
        return endsSentence(currentItem) && isLikelyStandaloneLine(line);
    }

    private boolean isSubSectionTitle(String line, ParentBlock currentParent) {
        if (!SUB_SECTION_SECTIONS.contains(currentParent.section())) {
            return false;
        }
        if (!isInitialSubSectionTitle(line)) {
            return false;
        }
        return currentParent.lines().size() <= 1 && isLikelyStandaloneLine(line);
    }

    private boolean isInitialSubSectionTitle(String line) {
        if (CONTACT_OR_LINK.matcher(line).matches() || TIME_RANGE.matcher(line).matches()) {
            return false;
        }
        if (BULLET_PREFIX.matcher(line).matches() || NOISE_LABEL.matcher(line).matches()) {
            return false;
        }
        if (DETAIL_START.matcher(line).matches()) {
            return false;
        }
        if (ENDS_WITH_SENTENCE.matcher(line).matches()) {
            return false;
        }
        return line.length() <= 40 && isLikelyStandaloneLine(line);
    }

    private boolean isLikelyStandaloneLine(String line) {
        if (line.isBlank()) {
            return false;
        }
        if (line.length() > 48) {
            return false;
        }
        return !(line.contains("：") || line.contains(":") || line.endsWith("。"));
    }

    private boolean endsSentence(String value) {
        return value.endsWith("。") || value.endsWith("；") || value.endsWith(";")
            || value.endsWith(".") || value.endsWith("!") || value.endsWith("！");
    }

    private String detectSection(String line) {
        String normalized = normalizeSectionCandidate(line);
        for (Map.Entry<String, String> entry : SECTION_ALIASES.entrySet()) {
            String alias = normalizeSectionCandidate(entry.getKey());
            if (normalized.equals(alias) || normalized.contains(alias) || alias.contains(normalized)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String normalizeSectionCandidate(String value) {
        String normalized = value == null ? "" : value;
        normalized = normalized.replace('\u00A0', ' ');
        normalized = LEADING_NOISE.matcher(normalized).replaceFirst("");
        normalized = TRAILING_TITLE_PUNCT.matcher(normalized).replaceFirst("");
        normalized = normalized.replace("|", "");
        normalized = normalized.replace("丨", "");
        normalized = normalized.replace("·", "");
        normalized = normalized.replace("•", "");
        normalized = normalized.replace("Â", "");
        normalized = normalized.replaceAll("\\s+", "");
        return normalized.trim();
    }

    private List<String> splitLineByEmbeddedSection(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return List.of();
        }

        List<String> fragments = new ArrayList<>();
        String remaining = rawLine.trim();
        while (!remaining.isBlank()) {
            SplitPoint splitPoint = findEmbeddedSection(remaining);
            if (splitPoint == null) {
                fragments.add(remaining);
                break;
            }

            String prefix = remaining.substring(0, splitPoint.index()).trim();
            if (!prefix.isBlank()) {
                fragments.add(prefix);
            }
            remaining = remaining.substring(splitPoint.index()).trim();

            int nextIndex = findNextEmbeddedSectionIndex(remaining, splitPoint.alias());
            if (nextIndex < 0) {
                fragments.add(remaining);
                break;
            }

            fragments.add(remaining.substring(0, nextIndex).trim());
            remaining = remaining.substring(nextIndex).trim();
        }
        return fragments;
    }

    private SplitPoint findEmbeddedSection(String value) {
        String bestAlias = null;
        int bestIndex = Integer.MAX_VALUE;

        for (String alias : SECTION_ALIASES.keySet()) {
            int index = value.indexOf(alias);
            if (index > 0 && index < bestIndex && looksLikeSectionBoundary(value, index)) {
                bestAlias = alias;
                bestIndex = index;
            }
        }

        if (bestAlias == null) {
            return null;
        }
        return new SplitPoint(bestAlias, bestIndex);
    }

    private int findNextEmbeddedSectionIndex(String value, String currentAlias) {
        int bestIndex = Integer.MAX_VALUE;
        for (String alias : SECTION_ALIASES.keySet()) {
            if (alias.equals(currentAlias)) {
                continue;
            }
            int index = value.indexOf(alias);
            if (index > 0 && index < bestIndex && looksLikeSectionBoundary(value, index)) {
                bestIndex = index;
            }
        }
        return bestIndex == Integer.MAX_VALUE ? -1 : bestIndex;
    }

    private boolean looksLikeSectionBoundary(String value, int index) {
        if (index <= 0 || index >= value.length()) {
            return false;
        }
        char previous = value.charAt(index - 1);
        return Character.isWhitespace(previous)
            || previous == '|'
            || previous == '｜'
            || previous == '·'
            || previous == '•'
            || previous == ':'
            || previous == '：';
    }

    private List<String> buildTags(String section, String subSectionTitle, boolean child) {
        Set<String> tags = new LinkedHashSet<>();
        tags.add(section);
        if (subSectionTitle != null && !subSectionTitle.isBlank()) {
            tags.add(subSectionTitle);
        }
        if (child) {
            tags.add("child");
        }
        return new ArrayList<>(tags);
    }

    private Map<String, Object> buildMetadata(ResumeDocument document, Integer page, ChunkType chunkType, String section,
                                              String subSectionTitle) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("candidateId", document.getCandidate().getId());
        metadata.put("docId", document.getId());
        metadata.put("page", page);
        metadata.put("chunkType", chunkType.name());
        metadata.put("versionNo", document.getVersionNo());
        metadata.put("section", section);
        if (subSectionTitle != null && !subSectionTitle.isBlank()) {
            metadata.put("subSectionTitle", subSectionTitle);
        }
        return metadata;
    }

    private String enrichContent(String section, String subSectionTitle, String rawContent) {
        String normalizedSubSection = subSectionTitle == null || subSectionTitle.isBlank()
            ? DEFAULT_SUB_SECTION
            : subSectionTitle;
        return "[板块:" + section + "][子项:" + normalizedSubSection + "] 内容:" + rawContent;
    }

    private String normalizeLine(String rawLine) {
        String normalized = rawLine == null ? "" : rawLine.replace('\u00A0', ' ').trim();
        normalized = normalized.replace("Â·", "·");
        normalized = normalized.replace("Â", "");
        normalized = normalized.replace("•", "·");
        normalized = normalized.replace("●", "·");
        normalized = normalized.replaceAll("[\\uF000-\\uF8FF]", "");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        normalized = LEADING_NOISE.matcher(normalized).replaceFirst("").trim();
        return normalized;
    }

    private record IndexedLine(String text, int page) {
    }

    private record SplitPoint(String alias, int index) {
    }

    private static final class ParentBlock {

        private final String section;
        private final String subSectionTitle;
        private final int page;
        private final List<String> lines = new ArrayList<>();

        private ParentBlock(String section, String subSectionTitle, int page) {
            this.section = section;
            this.subSectionTitle = subSectionTitle;
            this.page = page;
        }

        private void addLine(String line) {
            lines.add(line);
        }

        private String section() {
            return section;
        }

        private String subSectionTitle() {
            return subSectionTitle;
        }

        private int page() {
            return page;
        }

        private List<String> lines() {
            return lines;
        }

        private String parentContent() {
            if (subSectionTitle == null || subSectionTitle.isBlank()) {
                return String.join("\n", lines);
            }
            if (lines.isEmpty()) {
                return subSectionTitle;
            }
            return subSectionTitle + "\n" + String.join("\n", lines);
        }
    }
}
