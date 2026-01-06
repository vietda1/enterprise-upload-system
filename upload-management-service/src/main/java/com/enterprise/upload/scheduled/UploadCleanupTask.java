package com.enterprise.upload.scheduled;

import com.enterprise.upload.model.Upload;
import com.enterprise.upload.repository.UploadRepository;
import com.enterprise.upload.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UploadCleanupTask {
    
    private final UploadRepository uploadRepository;
    private final MinioService minioService;
    
    // Run every hour
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredPendingUploads() {
        log.info("Starting cleanup of expired pending uploads");
        
        // Delete pending uploads older than 24 hours
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(24);
        List<Upload> expiredUploads = uploadRepository.findExpiredPendingUploads(cutoffDate);
        
        log.info("Found {} expired pending uploads", expiredUploads.size());
        
        for (Upload upload : expiredUploads) {
            try {
                // Delete from MinIO if exists
                if (minioService.objectExists(upload.getObjectKey())) {
                    minioService.deleteObject(upload.getObjectKey());
                }
                
                // Delete from database
                uploadRepository.delete(upload);
                
                log.info("Deleted expired upload: {}", upload.getId());
            } catch (Exception e) {
                log.error("Failed to delete expired upload: {}", upload.getId(), e);
            }
        }
        
        log.info("Cleanup completed");
    }
}