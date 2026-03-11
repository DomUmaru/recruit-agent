package com.recruit.agent.resume.service.impl;

import com.recruit.agent.candidate.model.Candidate;
import com.recruit.agent.candidate.repository.CandidateRepository;
import com.recruit.agent.resume.dto.ResumeUploadRequest;
import com.recruit.agent.resume.dto.ResumeUploadResponse;
import com.recruit.agent.resume.model.ResumeDocument;
import com.recruit.agent.resume.model.ResumeDocumentStatus;
import com.recruit.agent.resume.model.ResumeParseType;
import com.recruit.agent.resume.repository.ResumeDocumentRepository;
import com.recruit.agent.resume.service.ResumeApplicationService;
import com.recruit.agent.resume.service.ResumeIngestionService;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历应用服务实现。
 */
@Service
public class ResumeApplicationServiceImpl implements ResumeApplicationService {

    private final CandidateRepository candidateRepository;
    private final ResumeDocumentRepository resumeDocumentRepository;
    private final ResumeIngestionService resumeIngestionService;

    @Value("${app.resume.upload-dir:data/uploads/resumes}")
    private String uploadDir;

    public ResumeApplicationServiceImpl(CandidateRepository candidateRepository,
                                        ResumeDocumentRepository resumeDocumentRepository,
                                        ResumeIngestionService resumeIngestionService) {
        this.candidateRepository = candidateRepository;
        this.resumeDocumentRepository = resumeDocumentRepository;
        this.resumeIngestionService = resumeIngestionService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeUploadResponse uploadResume(ResumeUploadRequest request, MultipartFile file) throws IOException {
        validateFile(file);

        Candidate candidate = candidateRepository.findById(request.getCandidateId())
            .orElseThrow(() -> new EntityNotFoundException("候选人不存在"));

        Path storedFilePath = storeFile(file, candidate.getId());

        resumeDocumentRepository.findByCandidateIdAndActiveVersion(candidate.getId(), true)
            .ifPresent(existing -> existing.setActiveVersion(false));

        ResumeDocument document = buildResumeDocument(candidate, file, storedFilePath);
        ResumeDocument savedDocument = resumeDocumentRepository.save(document);
        resumeIngestionService.ingest(savedDocument, storedFilePath);

        ResumeUploadResponse response = new ResumeUploadResponse();
        response.setDocumentId(savedDocument.getId());
        response.setCandidateId(candidate.getId());
        response.setFileName(savedDocument.getFileName());
        response.setVersionNo(savedDocument.getVersionNo());
        response.setStatus(savedDocument.getStatus().name());
        response.setFileStorageKey(savedDocument.getFileStorageKey());
        response.setCreatedAt(savedDocument.getCreatedAt());
        return response;
    }

    private void validateFile(MultipartFile file) {
        if (Objects.isNull(file) || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
    }

    private Path storeFile(MultipartFile file, String candidateId) throws IOException {
        String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "resume.pdf");
        String extension = extractExtension(originalFilename);
        String storedFileName = UUID.randomUUID() + extension;
        Path directory = Paths.get(uploadDir, candidateId);
        Files.createDirectories(directory);
        Path targetPath = directory.resolve(storedFileName);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return targetPath;
    }

    private ResumeDocument buildResumeDocument(Candidate candidate, MultipartFile file, Path storedFilePath) throws IOException {
        ResumeDocument document = new ResumeDocument();
        document.setCandidate(candidate);
        document.setFileName(Objects.requireNonNullElse(file.getOriginalFilename(), storedFilePath.getFileName().toString()));
        document.setFileStorageKey(storedFilePath.toString().replace('\\', '/'));
        document.setFileChecksum(calculateSha256(storedFilePath));
        document.setVersionNo(nextVersionNo(candidate.getId()));
        document.setStatus(ResumeDocumentStatus.UPLOADED);
        document.setParseType(ResumeParseType.UNKNOWN);
        document.setActiveVersion(true);
        return document;
    }

    private Integer nextVersionNo(String candidateId) {
        return resumeDocumentRepository.findTopByCandidateIdOrderByVersionNoDesc(candidateId)
            .map(existing -> existing.getVersionNo() + 1)
            .orElse(1);
    }

    private String calculateSha256(Path storedFilePath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(storedFilePath)) {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(messageDigest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 算法不可用", ex);
        }
    }

    private String extractExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index >= 0 ? fileName.substring(index) : "";
    }
}
