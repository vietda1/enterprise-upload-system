package com.enterprise.upload.service.impl;

import com.enterprise.upload.dto.request.*;
import com.enterprise.upload.dto.response.*;
import com.enterprise.upload.exception.InvalidUploadStateException;
import com.enterprise.upload.exception.UploadNotFoundException;
import com.enterprise.upload.mapper.UploadMapper;
import com.enterprise.upload.model.*;
import com.enterprise.upload.model.enums.UploadStatus;
import com.enterprise.upload.repository.*;
import com.enterprise.upload.service.*;
import com.enterprise.upload.util.UploadIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final UploadRepository           uploadRepository;
    private final PresignedUrlLogRepository  presignedUrlLogRepository;
    private final ValidationDatasetResultRepository validationRepository;
    // private final IngestionJobRepository     ingestionJobRepository;
    private final MinioService               minioService;
    private final KafkaPublisherService      kafkaPublisherService;
    private final UploadEventService         uploadEventService;
    private final DatasetConfigRepository    datasetConfigRepository;
    private final UploadMapper               uploadMapper;

    @Value("${minio.presigned-url-expiry-minutes:15}")
    private int presignedUrlExpiryMinutes;

    @Value("${minio.bucket-name:enterprise-uploads}")
    private String bucketName;

    // ─── 1. Request Presigned URL ─────────────────────────────────────────────

    @Override
    @Transactional
    public PresignedUrlResponse requestPresignedUrl(PresignedUrlRequest req, String userId) {
        // Validate dataset config exists
        datasetConfigRepository.findByCodeAndIsActiveTrue(req.getDatasetType())
            .orElseThrow(() -> new InvalidUploadStateException(
                "Dataset type not found or inactive: " + req.getDatasetType()));

        String uploadId = UploadIdGenerator.generate();
        String objectKey = buildObjectKey(userId, req.getDepartmentId(), uploadId, req.getFileName());
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(presignedUrlExpiryMinutes);

        // Generate presigned PUT URL from MinIO
        String presignedUrl = minioService.generatePresignedPutUrl(objectKey, presignedUrlExpiryMinutes);

        // Persist upload record
        Upload upload = Upload.builder()
            .uploadId(uploadId)
            .userId(userId)
            .departmentId(req.getDepartmentId())
            .fileName(sanitizeFileName(req.getFileName()))
            .originalFileName(req.getFileName())
            .fileSize(req.getFileSize())
            .fileType(req.getFileType())
            .mimeType(req.getMimeType())
            .objectKey(objectKey)
            .bucketName(bucketName)
            .datasetType(req.getDatasetType())
            .targetDatabase(req.getTargetDatabase())
            .targetTable(req.getTargetTable())
            .description(req.getDescription())
            .tags(req.getTags())
            .metadata(req.getMetadata())
            .status(UploadStatus.PENDING)
            .presignedUrlExpiresAt(expiresAt)
            .build();

        uploadRepository.save(upload);

        // Log presigned URL
        presignedUrlLogRepository.save(PresignedUrlLog.builder()
            .uploadId(uploadId)
            .urlType("PUT")
            .objectKey(objectKey)
            .expiresAt(expiresAt)
            .build());

        // Audit event
        uploadEventService.record(upload, "PRESIGNED_URL_REQUESTED", "UPLOAD_SERVICE",
            userId, null, null, "PENDING",
            "Presigned URL generated for file: " + req.getFileName(),
            Map.of("objectKey", objectKey, "expiresAt", expiresAt.toString()), null);

        log.info("Presigned URL generated: uploadId={}, userId={}, file={}", uploadId, userId, req.getFileName());

        return PresignedUrlResponse.builder()
            .uploadId(uploadId)
            .presignedUrl(presignedUrl)
            .objectKey(objectKey)
            .bucketName(bucketName)
            .expiresAt(expiresAt)
            .expiryMinutes(presignedUrlExpiryMinutes)
            .build();
    }

    // ─── 2. Confirm Upload ────────────────────────────────────────────────────

    @Override
    @Transactional
    public UploadResponse confirmUpload(String uploadId, ConfirmUploadRequest req, String userId) {
        Upload upload = findAndCheckOwner(uploadId, userId);

        if (upload.getStatus() != UploadStatus.PENDING) {
            throw new InvalidUploadStateException(
                "Upload already confirmed or in state: " + upload.getStatus());
        }
        if (upload.isExpiredPresignedUrl()) {
            upload.setStatus(UploadStatus.EXPIRED);
            uploadRepository.save(upload);
            throw new InvalidUploadStateException("Presigned URL has expired for upload: " + uploadId);
        }

        // Verify file actually exists in MinIO
        if (!minioService.objectExists(upload.getObjectKey())) {
            throw new InvalidUploadStateException("File not found in storage: " + upload.getObjectKey());
        }

        String prevStatus = upload.getStatus().name();
        upload.setStatus(UploadStatus.UPLOADED);
        upload.setEtag(req.getEtag());
        if (req.getFileSize() != null) upload.setFileSize(req.getFileSize());
        uploadRepository.save(upload);

        // Mark presigned URL as used
        presignedUrlLogRepository.findByUploadIdOrderByCreatedAtDesc(uploadId).stream()
            .filter(l -> !l.hasBeenUsed())
            .findFirst()
            .ifPresent(l -> { l.setUsedAt(LocalDateTime.now()); presignedUrlLogRepository.save(l); });

        uploadEventService.record(upload, "FILE_UPLOAD_COMPLETED", "UPLOAD_SERVICE",
            userId, null, prevStatus, "UPLOADED",
            "File confirmed in MinIO storage",
            Map.of("etag", req.getEtag() != null ? req.getEtag() : ""), null);

        // Publish Kafka event → triggers Validation Service
        kafkaPublisherService.publishUploadCompleted(upload);

        log.info("Upload confirmed: uploadId={}, status=UPLOADED", uploadId);
        return uploadMapper.toResponse(upload);
    }

    // ─── 3. Approve / Reject ─────────────────────────────────────────────────

    @Override
    @Transactional
    public UploadResponse approveUpload(String uploadId, ApproveUploadRequest req, String approverId) {
        Upload upload = findUpload(uploadId);

        if (upload.getStatus() != UploadStatus.PENDING_APPROVAL) {
            throw new InvalidUploadStateException(
                "Upload must be in PENDING_APPROVAL state to approve. Current: " + upload.getStatus());
        }

        String prevStatus = upload.getStatus().name();
        upload.setStatus(UploadStatus.APPROVED);
        upload.setApprovedBy(approverId);
        upload.setApprovedAt(LocalDateTime.now());
        uploadRepository.save(upload);

        uploadEventService.record(upload, "APPROVED", "FRONTEND",
            approverId, "ROLE_DEPARTMENT_MANAGER", prevStatus, "APPROVED",
            req.getComment() != null ? req.getComment() : "Upload approved",
            null, null);

        kafkaPublisherService.publishUploadStatusChanged(upload, prevStatus);
        log.info("Upload approved: uploadId={}, approver={}", uploadId, approverId);
        return uploadMapper.toResponse(upload);
    }

    @Override
    @Transactional
    public UploadResponse rejectUpload(String uploadId, RejectUploadRequest req, String rejecterId) {
        Upload upload = findUpload(uploadId);

        if (upload.getStatus() != UploadStatus.PENDING_APPROVAL
            && upload.getStatus() != UploadStatus.VALIDATED) {
            throw new InvalidUploadStateException(
                "Cannot reject upload in state: " + upload.getStatus());
        }

        String prevStatus = upload.getStatus().name();
        upload.setStatus(UploadStatus.REJECTED);
        upload.setRejectedBy(rejecterId);
        upload.setRejectedAt(LocalDateTime.now());
        upload.setRejectionReason(req.getReason());
        uploadRepository.save(upload);

        uploadEventService.record(upload, "REJECTED", "FRONTEND",
            rejecterId, "ROLE_DEPARTMENT_MANAGER", prevStatus, "REJECTED",
            req.getReason(), null, null);

        log.info("Upload rejected: uploadId={}, rejector={}, reason={}", uploadId, rejecterId, req.getReason());
        return uploadMapper.toResponse(upload);
    }

    // ─── 4. Query ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UploadResponse getUpload(String uploadId, String userId) {
        Upload upload = findUpload(uploadId);
        return uploadMapper.toResponse(upload);
    }

    @Override
    @Transactional(readOnly = true)
    public UploadPageResponse searchUploads(UploadSearchRequest req, String userId) {
        Sort sort = Sort.by(
            "asc".equalsIgnoreCase(req.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC,
            req.getSortBy());
        PageRequest pageable = PageRequest.of(req.getPage(), req.getSize(), sort);

        Page<Upload> page = uploadRepository.search(
            req.getUserId(), req.getDepartmentId(),
            req.getStatus(), req.getDatasetType(),
            req.getFrom(), req.getTo(), pageable);

        return UploadPageResponse.builder()
            .content(page.getContent().stream().map(uploadMapper::toResponse).toList())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .last(page.isLast())
            .build();
    }

    // ─── 5. Delete (soft) ────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteUpload(String uploadId, String userId) {
        Upload upload = findAndCheckOwner(uploadId, userId);
        if (upload.isSoftDeleted()) return;

        String prevStatus = upload.getStatus().name();
        upload.setDeletedAt(LocalDateTime.now());
        upload.setStatus(UploadStatus.DELETED);
        uploadRepository.save(upload);

        uploadEventService.record(upload, "DELETED", "FRONTEND",
            userId, null, prevStatus, "DELETED",
            "Upload soft-deleted by user", null, null);

        log.info("Upload soft-deleted: uploadId={}, userId={}", uploadId, userId);
    }

    // ─── 6. Share ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UploadResponse shareUpload(String uploadId, ShareUploadRequest req, String userId) {
        Upload upload = findAndCheckOwner(uploadId, userId);

        if (req.getSharedWithUserId() == null && req.getSharedWithDepartment() == null) {
            throw new InvalidUploadStateException("Must specify sharedWithUserId or sharedWithDepartment");
        }

        UploadShare share = UploadShare.builder()
            .sharedWithUserId(req.getSharedWithUserId())
            .sharedWithDepartment(req.getSharedWithDepartment())
            .permission(req.getPermission())
            .sharedBy(userId)
            .expiresAt(req.getExpiresAt())
            .build();
        upload.addShare(share);
        uploadRepository.save(upload);

        uploadEventService.record(upload, "SHARED", "FRONTEND", userId, null,
            null, null,
            "Upload shared with " + (req.getSharedWithUserId() != null
                ? "user: " + req.getSharedWithUserId()
                : "department: " + req.getSharedWithDepartment()),
            Map.of("permission", req.getPermission().name()), null);

        return uploadMapper.toResponse(upload);
    }

    // ─── 7. Validation Result ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ValidationResultResponse getValidationResult(String uploadId) {
        findUpload(uploadId); // verify exists
        return validationRepository
            .findFirstByUpload_UploadIdOrderByValidationRunDesc(uploadId)
            .map(uploadMapper::toValidationResponse)
            .orElseThrow(() -> new UploadNotFoundException("No validation result for upload: " + uploadId));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Upload findUpload(String uploadId) {
        return uploadRepository.findByUploadId(uploadId)
            .orElseThrow(() -> new UploadNotFoundException(uploadId));
    }

    private Upload findAndCheckOwner(String uploadId, String userId) {
        Upload upload = findUpload(uploadId);
        if (!upload.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own upload: " + uploadId);
        }
        return upload;
    }

    private String buildObjectKey(String userId, String deptId, String uploadId, String fileName) {
        String date = LocalDateTime.now().toString().substring(0, 7).replace("-", "/"); // YYYY/MM
        String dept = deptId != null ? deptId : "NO-DEPT";
        return String.format("uploads/%s/%s/%s/%s/%s", date, dept, userId, uploadId, sanitizeFileName(fileName));
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}
