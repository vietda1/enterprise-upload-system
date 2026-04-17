package com.enterprise.upload.kafka;

import com.enterprise.upload.model.enums.UploadStatus;
import com.enterprise.upload.repository.UploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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

        uploadRepository.findByUploadId(uploadId).ifPresent(upload -> {
            if ("VALID".equals(status)) {
                upload.setStatus(UploadStatus.VALIDATED);
            } else {
                upload.setStatus(UploadStatus.VALIDATION_FAILED);
            }

            uploadRepository.save(upload);
            log.info("Updated upload {} with validation result: {}", uploadId, status);
        });
    }

    @KafkaListener(topics = "ingestion.completed", groupId = "upload-service-group")
    public void handleIngestionCompleted(Map<String, Object> event) {
        log.info("Received ingestion.completed event: {}", event);

        String uploadId = (String) event.get("uploadId");
        String status = (String) event.get("status");

        uploadRepository.findByUploadId(uploadId).ifPresent(upload -> {
            if ("COMPLETED".equals(status)) {
                upload.setStatus(UploadStatus.COMPLETED);
            } else {
                upload.setStatus(UploadStatus.FAILED);
            }

            uploadRepository.save(upload);
            log.info("Updated upload {} with ingestion result: {}", uploadId, status);
        });
    }
}
