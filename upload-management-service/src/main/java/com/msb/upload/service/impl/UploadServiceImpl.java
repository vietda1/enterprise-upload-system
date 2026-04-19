package com.msb.upload.service.impl;

import com.msb.upload.dto.request.*;
import com.msb.upload.dto.response.*;
import com.msb.upload.exception.DuplicateUploadException;
import com.msb.upload.exception.InvalidUploadStateException;
import com.msb.upload.exception.UploadNotFoundException;
import com.msb.upload.mapper.UploadMapper;
import com.msb.upload.model.*;
import com.msb.upload.model.enums.UploadStatus;
import com.msb.upload.repository.*;
import com.msb.upload.security.UserPrincipal;
import com.msb.upload.service.*;
import com.msb.upload.util.UploadIdGenerator;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final UploadRepository                   uploadRepository;
    private final PresignedUrlLogRepository          presignedUrlLogRepository;
    private final ValidationDatasetResultRepository  validationRepository;
    private final MinioService                       minioService;
    private final KafkaPublisherService              kafkaPublisherService;
    private final UploadEventService                 uploadEventService;
    private final DatasetConfigRepository            datasetConfigRepository;
    private final UploadMapper                       uploadMapper;

    @Value("${aws.s3.presigned-url-expiry-minutes:15}")
    private int presignedUrlExpiryMinutes;

    @Value("${aws.s3.bucket-name:enterprise-uploads}")
    private String bucketName;

    // Trạng thái terminal: không tính là "đang tồn tại" khi kiểm tra dedup
    private static final List<UploadStatus> TERMINAL_STATUSES = List.of(
        UploadStatus.DELETED, UploadStatus.EXPIRED,
        UploadStatus.REJECTED, UploadStatus.FAILED,
        UploadStatus.VALIDATION_FAILED
    );

    // ─── 1. Request Presigned URL ─────────────────────────────────────────────

    @Override
    @Transactional
    public PresignedUrlResponse requestPresignedUrl(PresignedUrlRequest req, UserPrincipal principal) {
        String userId = principal.getUserId();
        String effectiveDeptId = resolveEffectiveDeptId(req.getDepartmentId(), principal);

        checkDepartmentPermission(req.getDepartmentId(), principal);
        datasetConfigRepository.findByCodeAndIsActiveTrue(req.getDatasetType())
            .orElseThrow(() -> new InvalidUploadStateException(
                "Dataset type not found or inactive: " + req.getDatasetType()));
        checkDuplicate(req.getChecksum(), effectiveDeptId, req.getDatasetType());

        String uploadId  = UploadIdGenerator.generate();
        String objectKey = buildObjectKey(userId, effectiveDeptId, uploadId, req.getFileName());
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(presignedUrlExpiryMinutes);

        String presignedUrl = minioService.generatePresignedPutUrl(objectKey, presignedUrlExpiryMinutes);

        Upload upload = Upload.builder()
            .uploadId(uploadId)
            .userId(userId)
            .departmentId(effectiveDeptId)
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
            .checksum(req.getChecksum())
            .checksumAlgorithm(resolveAlgorithm(req.getChecksumAlgorithm()))
            .status(UploadStatus.PENDING)
            .presignedUrlExpiresAt(expiresAt)
            .build();
        uploadRepository.save(upload);

        presignedUrlLogRepository.save(PresignedUrlLog.builder()
            .uploadId(uploadId).urlType("PUT")
            .objectKey(objectKey).expiresAt(expiresAt).build());

        uploadEventService.record(upload, "PRESIGNED_URL_REQUESTED", "UPLOAD_SERVICE",
            userId, null, null, "PENDING",
            "Presigned URL generated for file: " + req.getFileName(),
            Map.of("objectKey", objectKey, "expiresAt", expiresAt.toString()), null);

        log.info("Presigned URL generated: uploadId={}, userId={}, file={}", uploadId, userId, req.getFileName());

        return PresignedUrlResponse.builder()
            .uploadId(uploadId).presignedUrl(presignedUrl)
            .objectKey(objectKey).bucketName(bucketName)
            .expiresAt(expiresAt).expiryMinutes(presignedUrlExpiryMinutes)
            .build();
    }

    // ─── 2. Confirm Upload ────────────────────────────────────────────────────

    @Override
    @Transactional
    public UploadResponse confirmUpload(String uploadId, ConfirmUploadRequest req, UserPrincipal principal) {
        Upload upload = findAndCheckAccess(uploadId, principal);

        if (upload.getStatus() != UploadStatus.PENDING) {
            throw new InvalidUploadStateException(
                "Upload already confirmed or in state: " + upload.getStatus());
        }
        if (upload.isExpiredPresignedUrl()) {
            upload.setStatus(UploadStatus.EXPIRED);
            uploadRepository.save(upload);
            throw new InvalidUploadStateException("Presigned URL has expired for upload: " + uploadId);
        }
        if (!minioService.objectExists(upload.getObjectKey())) {
            throw new InvalidUploadStateException("File not found in storage: " + upload.getObjectKey());
        }

        String prevStatus = upload.getStatus().name();
        upload.setStatus(UploadStatus.UPLOADED);
        upload.setEtag(req.getEtag());
        if (req.getFileSize() != null) upload.setFileSize(req.getFileSize());
        uploadRepository.save(upload);

        presignedUrlLogRepository.findByUploadIdOrderByCreatedAtDesc(uploadId).stream()
            .filter(l -> !l.hasBeenUsed()).findFirst()
            .ifPresent(l -> { l.setUsedAt(LocalDateTime.now()); presignedUrlLogRepository.save(l); });

        uploadEventService.record(upload, "FILE_UPLOAD_COMPLETED", "UPLOAD_SERVICE",
            principal.getUserId(), null, prevStatus, "UPLOADED",
            "File confirmed in S3/ECS storage",
            Map.of("etag", req.getEtag() != null ? req.getEtag() : ""), null);

        kafkaPublisherService.publishUploadCompleted(upload);
        log.info("Upload confirmed: uploadId={}", uploadId);
        return uploadMapper.toResponse(upload);
    }

    // ─── 3. Approve / Reject ─────────────────────────────────────────────────

    @Override
    @Transactional
    public UploadResponse approveUpload(String uploadId, ApproveUploadRequest req, UserPrincipal principal) {
        Upload upload = findUpload(uploadId);

        if (upload.getStatus() != UploadStatus.PENDING_APPROVAL) {
            throw new InvalidUploadStateException(
                "Upload must be in PENDING_APPROVAL state to approve. Current: " + upload.getStatus());
        }

        String prevStatus = upload.getStatus().name();
        upload.setStatus(UploadStatus.APPROVED);
        upload.setApprovedBy(principal.getUserId());
        upload.setApprovedAt(LocalDateTime.now());
        uploadRepository.save(upload);

        uploadEventService.record(upload, "APPROVED", "FRONTEND",
            principal.getUserId(), roleLabel(principal), prevStatus, "APPROVED",
            req.getComment() != null ? req.getComment() : "Upload approved", null, null);

        kafkaPublisherService.publishUploadStatusChanged(upload, prevStatus);
        log.info("Upload approved: uploadId={}, approver={}", uploadId, principal.getUserId());
        return uploadMapper.toResponse(upload);
    }

    @Override
    @Transactional
    public UploadResponse rejectUpload(String uploadId, RejectUploadRequest req, UserPrincipal principal) {
        Upload upload = findUpload(uploadId);

        if (upload.getStatus() != UploadStatus.PENDING_APPROVAL
            && upload.getStatus() != UploadStatus.VALIDATED) {
            throw new InvalidUploadStateException("Cannot reject upload in state: " + upload.getStatus());
        }

        String prevStatus = upload.getStatus().name();
        upload.setStatus(UploadStatus.REJECTED);
        upload.setRejectedBy(principal.getUserId());
        upload.setRejectedAt(LocalDateTime.now());
        upload.setRejectionReason(req.getReason());
        uploadRepository.save(upload);

        uploadEventService.record(upload, "REJECTED", "FRONTEND",
            principal.getUserId(), roleLabel(principal), prevStatus, "REJECTED",
            req.getReason(), null, null);

        log.info("Upload rejected: uploadId={}, rejector={}", uploadId, principal.getUserId());
        return uploadMapper.toResponse(upload);
    }

    // ─── 4. Query ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UploadResponse getUpload(String uploadId, UserPrincipal principal) {
        return uploadMapper.toResponse(findUpload(uploadId));
    }

    @Override
    @Transactional(readOnly = true)
    public UploadPageResponse searchUploads(UploadSearchRequest req, UserPrincipal principal) {
        Sort sort = Sort.by(
            "asc".equalsIgnoreCase(req.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC,
            req.getSortBy());
        PageRequest pageable = PageRequest.of(req.getPage(), req.getSize(), sort);

        // Non-admin chỉ thấy upload của department mình nếu không truyền filter
        String effectiveDeptId = isAdmin(principal) ? req.getDepartmentId()
            : (req.getDepartmentId() != null ? req.getDepartmentId() : principal.getDepartmentId());

        Page<Upload> page = uploadRepository.search(
            req.getUserId(), effectiveDeptId,
            req.getStatus(), req.getDatasetType(),
            req.getFrom(), req.getTo(), pageable);

        return UploadPageResponse.builder()
            .content(page.getContent().stream().map(uploadMapper::toResponse).toList())
            .page(page.getNumber()).size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages()).last(page.isLast())
            .build();
    }

    // ─── 5. Delete (soft) ────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteUpload(String uploadId, UserPrincipal principal) {
        Upload upload = findAndCheckAccess(uploadId, principal);
        if (upload.isSoftDeleted()) return;

        String prevStatus = upload.getStatus().name();
        upload.setDeletedAt(LocalDateTime.now());
        upload.setStatus(UploadStatus.DELETED);
        uploadRepository.save(upload);

        uploadEventService.record(upload, "DELETED", "FRONTEND",
            principal.getUserId(), null, prevStatus, "DELETED",
            "Upload soft-deleted by user", null, null);

        log.info("Upload soft-deleted: uploadId={}", uploadId);
    }

    // ─── 6. Share ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UploadResponse shareUpload(String uploadId, ShareUploadRequest req, UserPrincipal principal) {
        Upload upload = findAndCheckAccess(uploadId, principal);

        if (req.getSharedWithUserId() == null && req.getSharedWithDepartment() == null) {
            throw new InvalidUploadStateException("Must specify sharedWithUserId or sharedWithDepartment");
        }

        UploadShare share = UploadShare.builder()
            .sharedWithUserId(req.getSharedWithUserId())
            .sharedWithDepartment(req.getSharedWithDepartment())
            .permission(req.getPermission())
            .sharedBy(principal.getUserId())
            .expiresAt(req.getExpiresAt())
            .build();
        upload.addShare(share);
        uploadRepository.save(upload);

        uploadEventService.record(upload, "SHARED", "FRONTEND",
            principal.getUserId(), null, null, null,
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
        findUpload(uploadId);
        return validationRepository
            .findFirstByUpload_UploadIdOrderByValidationRunDesc(uploadId)
            .map(uploadMapper::toValidationResponse)
            .orElseThrow(() -> new UploadNotFoundException("No validation result for upload: " + uploadId));
    }

    // ─── 8. Multipart Upload ──────────────────────────────────────────────────

    @Override
    @Transactional
    public MultipartInitiateResponse initiateMultipartUpload(MultipartInitiateRequest req, UserPrincipal principal) {
        String userId = principal.getUserId();
        String effectiveDeptId = resolveEffectiveDeptId(req.getDepartmentId(), principal);

        checkDepartmentPermission(req.getDepartmentId(), principal);
        datasetConfigRepository.findByCodeAndIsActiveTrue(req.getDatasetType())
            .orElseThrow(() -> new InvalidUploadStateException(
                "Dataset type not found or inactive: " + req.getDatasetType()));
        checkDuplicate(req.getChecksum(), effectiveDeptId, req.getDatasetType());

        String uploadId  = UploadIdGenerator.generate();
        String objectKey = buildObjectKey(userId, effectiveDeptId, uploadId, req.getFileName());

        String s3UploadId = minioService.initiateMultipartUpload(objectKey);

        long partSizeBytes = (long) req.getPartSizeMb() * 1024 * 1024;
        long fileSize      = req.getFileSize();
        int  totalParts    = (int) Math.ceil((double) fileSize / partSizeBytes);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(presignedUrlExpiryMinutes);

        List<PartUrlInfo> parts = new ArrayList<>();
        for (int i = 1; i <= totalParts; i++) {
            long startByte = (long) (i - 1) * partSizeBytes;
            long endByte   = Math.min(startByte + partSizeBytes - 1, fileSize - 1);
            parts.add(PartUrlInfo.builder()
                .partNumber(i)
                .uploadUrl(minioService.generatePresignedPartUrl(objectKey, s3UploadId, i, presignedUrlExpiryMinutes))
                .startByte(startByte).endByte(endByte)
                .partSize(endByte - startByte + 1)
                .build());
        }

        Map<String, Object> metadata = new HashMap<>();
        if (req.getMetadata() != null) metadata.putAll(req.getMetadata());
        metadata.put("s3UploadId", s3UploadId);
        metadata.put("totalParts", totalParts);

        Upload upload = Upload.builder()
            .uploadId(uploadId).userId(userId).departmentId(effectiveDeptId)
            .fileName(sanitizeFileName(req.getFileName())).originalFileName(req.getFileName())
            .fileSize(fileSize).fileType(req.getFileType()).mimeType(req.getMimeType())
            .objectKey(objectKey).bucketName(bucketName)
            .datasetType(req.getDatasetType())
            .targetDatabase(req.getTargetDatabase()).targetTable(req.getTargetTable())
            .description(req.getDescription()).tags(req.getTags()).metadata(metadata)
            .checksum(req.getChecksum())
            .checksumAlgorithm(resolveAlgorithm(req.getChecksumAlgorithm()))
            .status(UploadStatus.PENDING).presignedUrlExpiresAt(expiresAt)
            .build();
        uploadRepository.save(upload);

        uploadEventService.record(upload, "MULTIPART_UPLOAD_INITIATED", "UPLOAD_SERVICE",
            userId, null, null, "PENDING",
            String.format("Multipart upload initiated: %d parts for file: %s", totalParts, req.getFileName()),
            Map.of("s3UploadId", s3UploadId, "totalParts", totalParts, "partSizeMb", req.getPartSizeMb()), null);

        log.info("Multipart upload initiated: uploadId={}, parts={}, file={}", uploadId, totalParts, req.getFileName());

        return MultipartInitiateResponse.builder()
            .uploadId(uploadId).s3UploadId(s3UploadId).objectKey(objectKey).bucketName(bucketName)
            .fileSizeBytes(fileSize).partSizeBytes(partSizeBytes).totalParts(totalParts)
            .parts(parts).expiresAt(expiresAt)
            .build();
    }

    @Override
    @Transactional
    public UploadResponse completeMultipartUpload(String uploadId, CompleteMultipartRequest req, UserPrincipal principal) {
        Upload upload = findAndCheckAccess(uploadId, principal);

        if (upload.getStatus() != UploadStatus.PENDING) {
            throw new InvalidUploadStateException(
                "Upload must be in PENDING state to complete. Current: " + upload.getStatus());
        }
        String s3UploadId = upload.getMetadata() != null
            ? (String) upload.getMetadata().get("s3UploadId") : null;
        if (s3UploadId == null) {
            throw new InvalidUploadStateException("Upload was not initiated as multipart: " + uploadId);
        }

        String prevStatus = upload.getStatus().name();
        minioService.completeMultipartUpload(upload.getObjectKey(), s3UploadId, req.getParts());
        upload.setStatus(UploadStatus.UPLOADED);
        uploadRepository.save(upload);

        uploadEventService.record(upload, "FILE_UPLOAD_COMPLETED", "UPLOAD_SERVICE",
            principal.getUserId(), null, prevStatus, "UPLOADED",
            "Multipart upload completed in S3/ECS storage",
            Map.of("parts", req.getParts().size(), "s3UploadId", s3UploadId), null);

        kafkaPublisherService.publishUploadCompleted(upload);
        log.info("Multipart upload completed: uploadId={}, parts={}", uploadId, req.getParts().size());
        return uploadMapper.toResponse(upload);
    }

    @Override
    @Transactional
    public void abortMultipartUpload(String uploadId, UserPrincipal principal) {
        Upload upload = findAndCheckAccess(uploadId, principal);

        String s3UploadId = upload.getMetadata() != null
            ? (String) upload.getMetadata().get("s3UploadId") : null;
        if (s3UploadId != null) {
            minioService.abortMultipartUpload(upload.getObjectKey(), s3UploadId);
        }

        String prevStatus = upload.getStatus().name();
        upload.setStatus(UploadStatus.DELETED);
        upload.setDeletedAt(LocalDateTime.now());
        uploadRepository.save(upload);

        uploadEventService.record(upload, "MULTIPART_UPLOAD_ABORTED", "FRONTEND",
            principal.getUserId(), null, prevStatus, "DELETED",
            "Multipart upload aborted by user", null, null);

        log.info("Multipart upload aborted: uploadId={}", uploadId);
    }

    // ─── Permission & Dedup helpers ───────────────────────────────────────────

    private boolean isAdmin(UserPrincipal principal) {
        return principal.getRoles().contains("ROLE_ADMIN");
    }

    /**
     * Admin có thể upload cho bất kỳ department nào.
     * User thường chỉ được upload cho department của chính mình.
     * Trả lời câu hỏi "1 uploadId - 2 department?": KHÔNG.
     *   - uploadId thuộc về đúng 1 department (người tạo).
     *   - Department khác muốn upload cùng file phải tạo uploadId riêng.
     *   - Admin có thể tạo uploadId thay mặt bất kỳ department nào.
     *   - Chia sẻ quyền đọc thực hiện qua /share (UploadShare), KHÔNG phải upload.
     */
    private void checkDepartmentPermission(String targetDepartmentId, UserPrincipal principal) {
        if (isAdmin(principal)) return;
        if (targetDepartmentId == null) return;
        if (!targetDepartmentId.equals(principal.getDepartmentId())) {
            throw new AccessDeniedException(
                "You cannot upload for department '" + targetDepartmentId
                + "'. Your department: '" + principal.getDepartmentId() + "'");
        }
    }

    /**
     * Admin có thể truy cập bất kỳ uploadId nào.
     * User thường chỉ được truy cập upload do chính họ tạo.
     */
    private Upload findAndCheckAccess(String uploadId, UserPrincipal principal) {
        Upload upload = findUpload(uploadId);
        if (isAdmin(principal)) return upload;
        if (!upload.getUserId().equals(principal.getUserId())) {
            throw new AccessDeniedException("You do not own upload: " + uploadId);
        }
        return upload;
    }

    /**
     * Kiểm tra trùng file trong cùng department + datasetType.
     * Scope: intra-department — dept khác KHÔNG bị block dù checksum giống nhau.
     * Bỏ qua nếu checksum không được cung cấp (backward-compatible).
     */
    private void checkDuplicate(String checksum, String departmentId, String datasetType) {
        if (checksum == null || checksum.isBlank()) return;
        if (departmentId == null) return;
        uploadRepository.findDuplicates(checksum, departmentId, datasetType,
                TERMINAL_STATUSES, PageRequest.of(0, 1))
            .stream().findFirst()
            .ifPresent(existing -> {
                throw new DuplicateUploadException(
                    existing.getUploadId(), existing.getStatus().name(), existing.getFileName());
            });
    }

    /** Nếu user không truyền departmentId và không phải admin → dùng dept của chính họ. */
    private String resolveEffectiveDeptId(String requestedDeptId, UserPrincipal principal) {
        if (requestedDeptId != null) return requestedDeptId;
        return principal.getDepartmentId();
    }

    private String resolveAlgorithm(String algorithm) {
        return (algorithm != null && !algorithm.isBlank()) ? algorithm.toUpperCase() : "SHA256";
    }

    private String roleLabel(UserPrincipal principal) {
        return isAdmin(principal) ? "ROLE_ADMIN" : "ROLE_DEPARTMENT_MANAGER";
    }

    // ─── Standard helpers ─────────────────────────────────────────────────────

    private Upload findUpload(String uploadId) {
        return uploadRepository.findByUploadId(uploadId)
            .orElseThrow(() -> new UploadNotFoundException(uploadId));
    }

    private String buildObjectKey(String userId, String deptId, String uploadId, String fileName) {
        String date = LocalDateTime.now().toString().substring(0, 7).replace("-", "/");
        String dept = deptId != null ? deptId : "NO-DEPT";
        return String.format("uploads/%s/%s/%s/%s/%s", date, dept, userId, uploadId, sanitizeFileName(fileName));
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}