package com.enterprise.upload.service;

import com.enterprise.upload.dto.*;
import com.enterprise.upload.model.*;
import com.enterprise.upload.repository.UploadRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UploadService {
    
    private final UploadRepository uploadRepository;
    private final MinioService minioService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public UploadService(UploadRepository uploadRepository, 
                        MinioService minioService,
                        KafkaTemplate<String, Object> kafkaTemplate) {
        this.uploadRepository = uploadRepository;
        this.minioService = minioService;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @Transactional
    public PresignedUrlResponse generatePresignedUrl(
            PresignedUrlRequest request, 
            String userId) {
        
        // Generate unique object key
        String uploadId = UUID.randomUUID().toString();
        String objectKey = String.format("%s/%s/%s-%s",
                userId,
                request.getMetadata().getDepartment(),
                uploadId,
                request.getFileName()
        );
        
        // Generate presigned URL
        String presignedUrl = minioService.generatePresignedUrl(
                objectKey, 
                request.getFileType()
        );
        
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
        upload.setAccessLevel(AccessLevel.valueOf(
                request.getMetadata().getAccessLevel().toUpperCase()
        ));
        upload.setDatasetType(request.getMetadata().getDatasetType());
        upload.setTargetDatabase(request.getMetadata().getTargetDatabase());
        upload.setCreatedAt(LocalDateTime.now());
        
        // Store metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("description", request.getMetadata().getDescription());
        metadata.put("autoIngest", String.valueOf(
                request.getMetadata().getAutoIngest()
        ));
        upload.setMetadata(metadata);
        
        uploadRepository.save(upload);
        
        return new PresignedUrlResponse(
                uploadId, 
                presignedUrl, 
                objectKey, 
                3600
        );
    }
    
    @Transactional
    public void confirmUpload(String uploadId) {
        Upload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new RuntimeException("Upload not found"));
        
        // Verify file exists in MinIO
        StatObjectResponse stat = minioService.getObjectMetadata(
                upload.getObjectKey()
        );
        
        upload.setFileSize(stat.size());
        upload.setStatus(UploadStatus.UPLOADED);
        upload.setUploadedAt(LocalDateTime.now());
        uploadRepository.save(upload);
        
        // Publish event to Kafka for validation
        Map<String, Object> event = new HashMap<>();
        event.put("uploadId", uploadId);
        event.put("objectKey", upload.getObjectKey());
        event.put("datasetType", upload.getDatasetType());
        event.put("userId", upload.getUserId());
        
        kafkaTemplate.send("upload.completed", event);
    }
    
    public Upload getUploadStatus(String uploadId) {
        return uploadRepository.findById(uploadId)
                .orElseThrow(() -> new RuntimeException("Upload not found"));
    }
}