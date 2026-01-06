package com.enterprise.upload.service;

import com.enterprise.upload.dto.*;
import com.enterprise.upload.exception.ResourceNotFoundException;
import com.enterprise.upload.exception.UnauthorizedException;
import com.enterprise.upload.model.*;
import com.enterprise.upload.repository.UploadRepository;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {
    
    private final UploadRepository uploadRepository;
    private final MinioService minioService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Transactional
    public PresignedUrlResponse generatePresignedUrl(
            PresignedUrlRequest request, 
            String userId) {
        
        log.info("Generating presigned URL for user: {}, file: {}", userId, request.getFileName());
        
        // Generate unique ID and object key
        String uploadId = UUID.randomUUID().toString();
        String objectKey = buildObjectKey(userId, request.getMetadata().getDepartment(), uploadId, request.getFileName());
        
        // Generate presigned URL from MinIO
        String presignedUrl = minioService.generatePresignedUrl(objectKey, request.getFileType());
        
        // Create upload record
        Upload upload = new Upload();
        upload.setId(uploadId);
        upload.setUserId(userId);
        upload.setFileName(request.getFileName());
        upload.setFileType(request.getFileType());
        upload.setObjectKey(objectKey);
        upload.setBucketName("enterprise-uploads");
        upload.setStatus(UploadStatus.PENDING);
        upload.setDepartment(request.getMetadata().getDepartment());
        upload.setAccessLevel(AccessLevel.valueOf(request.getMetadata().getAccessLevel().toUpperCase()));
        upload.setDatasetType(request.getMetadata().getDatasetType());
        upload.setTargetDatabase(request.getMetadata().getTargetDatabase());
        
        // Store metadata
        Map<String, String> metadata = new HashMap<>();
        if (request.getMetadata().getDescription() != null) {
            metadata.put("description", request.getMetadata().getDescription());
        }
        if (request.getMetadata().getTags() != null) {
            metadata.put("tags", String.join(",", request.getMetadata().getTags()));
        }
        metadata.put("autoIngest", String.valueOf(request.getMetadata().getAutoIngest()));
        upload.setMetadata(metadata);
        
        uploadRepository.save(upload);
        
        log.info("Upload record created with ID: {}", uploadId);
        
        return new PresignedUrlResponse(uploadId, presignedUrl, objectKey, 3600);
    }
    
    @Transactional
    public void confirmUpload(String uploadId) {
        log.info("Confirming upload: {}", uploadId);
        
        Upload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload not found: " + uploadId));
        
        // Verify file exists in MinIO
        if (!minioService.objectExists(upload.getObjectKey())) {
            throw new RuntimeException("File not found in storage: " + upload.getObjectKey());
        }
        
        // Get file metadata
        StatObjectResponse stat = minioService.getObjectMetadata(upload.getObjectKey());
        
        // Update upload status
        upload.setFileSize(stat.size());
        upload.setStatus(UploadStatus.UPLOADED);
        upload.setUploadedAt(LocalDateTime.now());
        
        uploadRepository.save(upload);
        
        // Publish event to Kafka
        publishUploadCompletedEvent(upload);
        
        log.info("Upload confirmed: {}", uploadId);
    }
    
    private void publishUploadCompletedEvent(Upload upload) {
        Map<String, Object> event = new HashMap<>();
        event.put("uploadId", upload.getId());
        event.put("objectKey", upload.getObjectKey());
        event.put("datasetType", upload.getDatasetType());
        event.put("userId", upload.getUserId());
        event.put("department", upload.getDepartment());
        event.put("targetDatabase", upload.getTargetDatabase());
        event.put("timestamp", LocalDateTime.now().toString());
        
        kafkaTemplate.send("upload.completed", upload.getId(), event);
        log.info("Published upload.completed event for: {}", upload.getId());
    }
    
    @Transactional(readOnly = true)
    public UploadResponse getUploadStatus(String uploadId, String userId) {
        Upload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload not found: " + uploadId));
        
        // Check access permission
        if (!hasAccess(upload, userId)) {
            throw new UnauthorizedException("Access denied to upload: " + uploadId);
        }
        
        return convertToResponse(upload);
    }
    
    @Transactional(readOnly = true)
    public Page<UploadResponse> listUploads(String userId, String department, UploadFilterRequest filter) {
        Pageable pageable = PageRequest.of(
            filter.getPage(),
            filter.getSize(),
            Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy())
        );
        
        Page<Upload> uploads;
        
        if (filter.getSearchTerm() != null && !filter.getSearchTerm().isEmpty()) {
            uploads = uploadRepository.searchByFileName(filter.getSearchTerm(), pageable);
        } else {
            uploads = uploadRepository.findByFilters(
                userId,
                filter.getStatus(),
                filter.getDepartment(),
                filter.getDatasetType(),
                pageable
            );
        }
        
        return uploads.map(this::convertToResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<UploadResponse> getAccessibleUploads(String userId, String department, Pageable pageable) {
        Page<Upload> uploads = uploadRepository.findAccessibleUploads(userId, department, pageable);
        return uploads.map(this::convertToResponse);
    }
    
    @Transactional
    public void deleteUpload(String uploadId, String userId) {
        Upload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload not found: " + uploadId));
        
        // Only owner can delete
        if (!upload.getUserId().equals(userId)) {
            throw new UnauthorizedException("Only owner can delete upload");
        }
        
        // Delete from MinIO
        try {
            minioService.deleteObject(upload.getObjectKey());
        } catch (Exception e) {
            log.error("Failed to delete object from MinIO: {}", upload.getObjectKey(), e);
        }
        
        // Soft delete in database
        upload.setStatus(UploadStatus.DELETED);
        uploadRepository.save(upload);
        
        log.info("Upload deleted: {}", uploadId);
    }
    
    @Transactional(readOnly = true)
    public UploadStatsResponse getUploadStats(String userId) {
        Long total = uploadRepository.countByUserIdAndStatus(userId, null);
        Long pending = uploadRepository.countByUserIdAndStatus(userId, UploadStatus.PENDING);
        Long completed = uploadRepository.countByUserIdAndStatus(userId, UploadStatus.COMPLETED);
        Long failed = uploadRepository.countByUserIdAndStatus(userId, UploadStatus.FAILED);
        Long totalSize = uploadRepository.getTotalSizeByUserId(userId);
        
        return new UploadStatsResponse(
            total,
            pending,
            completed,
            failed,
            totalSize != null ? totalSize : 0L,
            formatFileSize(totalSize != null ? totalSize : 0L)
        );
    }
    
    public String getDownloadUrl(String uploadId, String userId) {
        Upload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload not found: " + uploadId));
        
        if (!hasAccess(upload, userId)) {
            throw new UnauthorizedException("Access denied");
        }
        
        return minioService.getDownloadUrl(upload.getObjectKey());
    }
    
    private boolean hasAccess(Upload upload, String userId) {
        // Owner always has access
        if (upload.getUserId().equals(userId)) {
            return true;
        }
        
        // Public access
        if (upload.getAccessLevel() == AccessLevel.PUBLIC) {
            return true;
        }
        
        // Shared access - need to check if user is in same department
        // This would require user context - simplified here
        return upload.getAccessLevel() == AccessLevel.SHARED;
    }
    
    private String buildObjectKey(String userId, String department, String uploadId, String fileName) {
        return String.format("%s/%s/%s-%s", userId, department, uploadId, fileName);
    }
    
    private UploadResponse convertToResponse(Upload upload) {
        UploadResponse response = new UploadResponse();
        response.setId(upload.getId());
        response.setUserId(upload.getUserId());
        response.setFileName(upload.getFileName());
        response.setFileType(upload.getFileType());
        response.setFileSize(upload.getFileSize());
        response.setStatus(upload.getStatus());
        response.setDepartment(upload.getDepartment());
        response.setAccessLevel(upload.getAccessLevel());
        response.setDatasetType(upload.getDatasetType());
        response.setTargetDatabase(upload.getTargetDatabase());
        response.setMetadata(upload.getMetadata());
        response.setCreatedAt(upload.getCreatedAt());
        response.setUploadedAt(upload.getUploadedAt());
        response.setValidatedAt(upload.getValidatedAt());
        response.setIngestedAt(upload.getIngestedAt());
        response.setValidationResult(upload.getValidationResult());
        response.setIngestionResult(upload.getIngestionResult());
        response.setErrorMessage(upload.getErrorMessage());
        return response;
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}