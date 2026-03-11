package com.recruit.agent.rag.chunk;

import com.recruit.agent.resume.model.ResumeDocument;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Parent-Child 简历分块服务实现。
 */
@Service
public class ParentChildResumeChunkingService implements ResumeChunkingService {

    private static final int CHILD_CHUNK_TARGET_LENGTH = 300;

    @Override
    public ResumeChunkingResult chunk(ResumeDocument document) {
        List<ResumeChunkDraft> chunks = new ArrayList<>();
        List<String> pages = parsePageTexts(document.getPageTextJson());
        int chunkOrder = 0;

        for (int i = 0; i < pages.size(); i++) {
            String pageText = safeText(pages.get(i));
            if (pageText.isBlank()) {
                continue;
            }

            String parentId = UUID.randomUUID().toString();

            ResumeChunkDraft parentChunk = new ResumeChunkDraft();
            parentChunk.setParentId(parentId);
            parentChunk.setChunkType(ChunkType.PARENT);
            parentChunk.setSection(detectSection(pageText));
            parentChunk.setPage(i + 1);
            parentChunk.setChunkOrder(chunkOrder++);
            parentChunk.setContent(pageText);
            parentChunk.setTags(List.of(parentChunk.getSection(), "page_" + (i + 1)));
            parentChunk.setMetadata(buildMetadata(document, i + 1, ChunkType.PARENT));
            chunks.add(parentChunk);

            List<String> childPieces = splitIntoChildChunks(pageText);
            for (String childPiece : childPieces) {
                ResumeChunkDraft childChunk = new ResumeChunkDraft();
                childChunk.setParentId(parentId);
                childChunk.setChunkType(ChunkType.CHILD);
                childChunk.setSection(parentChunk.getSection());
                childChunk.setPage(i + 1);
                childChunk.setChunkOrder(chunkOrder++);
                childChunk.setContent(childPiece);
                childChunk.setTags(List.of(parentChunk.getSection(), "page_" + (i + 1), "child"));
                childChunk.setMetadata(buildMetadata(document, i + 1, ChunkType.CHILD));
                chunks.add(childChunk);
            }
        }

        ResumeChunkingResult result = new ResumeChunkingResult();
        result.setChunks(chunks);
        result.setLog("已完成 Parent-Child Chunking，生成 " + chunks.size() + " 个分块");
        return result;
    }

    private List<String> parsePageTexts(String pageTextJson) {
        if (pageTextJson == null || pageTextJson.isBlank() || "[]".equals(pageTextJson.trim())) {
            return List.of();
        }

        String normalized = pageTextJson.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        if (normalized.isBlank()) {
            return List.of();
        }

        String[] segments = normalized.split("\",\\s*\"");
        List<String> pages = new ArrayList<>();
        for (String segment : segments) {
            String page = segment
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replaceAll("^\"", "")
                .replaceAll("\"$", "");
            pages.add(page);
        }
        return pages;
    }

    private List<String> splitIntoChildChunks(String pageText) {
        List<String> childChunks = new ArrayList<>();
        String[] paragraphs = pageText.split("\\n\\n");

        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            String trimmed = safeText(paragraph);
            if (trimmed.isBlank()) {
                continue;
            }

            if (current.length() + trimmed.length() + 2 > CHILD_CHUNK_TARGET_LENGTH && current.length() > 0) {
                childChunks.add(current.toString().trim());
                current.setLength(0);
            }

            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(trimmed);
        }

        if (current.length() > 0) {
            childChunks.add(current.toString().trim());
        }

        if (childChunks.isEmpty()) {
            childChunks.add(pageText);
        }
        return childChunks;
    }

    private String detectSection(String pageText) {
        String normalized = pageText.toLowerCase();
        if (normalized.contains("教育") || normalized.contains("学历") || normalized.contains("education")) {
            return "education";
        }
        if (normalized.contains("项目") || normalized.contains("project")) {
            return "project";
        }
        if (normalized.contains("工作") || normalized.contains("经历") || normalized.contains("experience")) {
            return "experience";
        }
        if (normalized.contains("技能") || normalized.contains("tech") || normalized.contains("stack")) {
            return "skills";
        }
        return "general";
    }

    private Map<String, Object> buildMetadata(ResumeDocument document, Integer page, ChunkType chunkType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("candidateId", document.getCandidate().getId());
        metadata.put("docId", document.getId());
        metadata.put("page", page);
        metadata.put("chunkType", chunkType.name());
        metadata.put("versionNo", document.getVersionNo());
        return metadata;
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }
}
