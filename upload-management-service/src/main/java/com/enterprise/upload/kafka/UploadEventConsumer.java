package com.enterprise.upload.kafka;

import com.enterprise.upload.model.UploadStatus;
import com.enterprise.upload.repository.UploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UploadEventConsumer {
    
    private final UploadRepository uploadRepository;
    
    @KafkaListener(topics = "validation.completed", groupId = "upload-service-group")
    public void handleValidationCompleted(Map<String, Object> event) {
        log.info("Received validation.completed event: {}", event);
        
        String uploadId = (String) event.get("uploadId");
        String status = (String) event.get("status");
        
        uploadRepository.findById(uploadId).ifPresent(upload -> {
            if ("VALID".equals(status)) {
                upload.setStatus(UploadStatus.VALID);
                upload.setValidatedAt(LocalDateTime.now());
            } else {
                upload.setStatus(UploadStatus.VALIDATION_FAILED);
            }
            
            upload.setValidationResult(event.toString());
            uploadRepository.save(upload);
            
            log.info("Updated upload {} with validation result: {}", uploadId, status);
        });
    }
    
    @KafkaListener(topics = "ingestion.completed", groupId = "upload-service-group")
    public void handleIngestionCompleted(Map<String, Object> event) {
        log.info("Received ingestion.completed event: {}", event);
        
        String uploadId = (String) event.get("uploadId");
        String status = (String) event.get("status");
        
        uploadRepository.findById(uploadId).ifPresent(upload -> {
            if ("COMPLETED".equals(status)) {
                upload.setStatus(UploadStatus.COMPLETED);
                upload.setIngestedAt(LocalDateTime.now());
            } else {
                upload.setStatus(UploadStatus.INGESTION_FAILED);
            }
            
            upload.setIngestionResult(event.toString());
            uploadRepository.save(upload);
            
            log.info("Updated upload {} with ingestion result: {}", uploadId, status);
        });
    }
}