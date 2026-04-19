package com.msb.upload.kafka;

import com.msb.upload.model.Upload;
import com.msb.upload.model.enums.UploadStatus;
import com.msb.upload.repository.UploadRepository;
import com.msb.upload.service.UploadEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationResultListener {

    private final UploadRepository    uploadRepository;
    private final UploadEventService  uploadEventService;

    @KafkaListener(
        topics   = "${kafka.topics.validation-completed:validation.completed}",
        groupId  = "upload-management-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onValidationCompleted(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        String uploadId = (String) payload.get("uploadId");
        String status   = (String) payload.get("status");
        log.info("Received validation.completed: uploadId={}, status={}", uploadId, status);

        uploadRepository.findByUploadId(uploadId).ifPresentOrElse(upload -> {
            String prevStatus = upload.getStatus().name();

            if ("PASSED".equalsIgnoreCase(status)) {
                handleValidationPassed(upload, payload, prevStatus);
            } else if ("FAILED".equalsIgnoreCase(status)) {
                upload.setStatus(UploadStatus.VALIDATION_FAILED);
                uploadRepository.save(upload);
                uploadEventService.record(upload, "VALIDATION_FAILED", "VALIDATION_SERVICE",
                    "system", "SYSTEM", prevStatus, "VALIDATION_FAILED",
                    "Validation failed: blockers=" + payload.get("blockerCount"),
                    payload, null);
            }
        }, () -> log.warn("Upload not found for validation result: {}", uploadId));
    }

    private void handleValidationPassed(Upload upload, Map<String, Object> payload, String prevStatus) {
        // Check if dataset config requires approval
        boolean requiresApproval = Boolean.TRUE.equals(payload.get("requiresApproval"));

        if (requiresApproval) {
            upload.setStatus(UploadStatus.PENDING_APPROVAL);
            uploadRepository.save(upload);
            uploadEventService.record(upload, "PENDING_APPROVAL_NOTIFIED", "VALIDATION_SERVICE",
                "system", "SYSTEM", prevStatus, "PENDING_APPROVAL",
                "Validation passed. Awaiting checker approval.",
                payload, null);
        } else {
            upload.setStatus(UploadStatus.VALIDATED);
            uploadRepository.save(upload);
            uploadEventService.record(upload, "VALIDATION_COMPLETED", "VALIDATION_SERVICE",
                "system", "SYSTEM", prevStatus, "VALIDATED",
                "Validation passed. Auto-approved for ingestion.",
                payload, null);
        }
    }

    @KafkaListener(
        topics   = "${kafka.topics.ingestion-completed:ingestion.completed}",
        groupId  = "upload-management-service"
    )
    @Transactional
    public void onIngestionCompleted(@Payload Map<String, Object> payload) {
        String uploadId = (String) payload.get("uploadId");
        String status   = (String) payload.get("status");
        log.info("Received ingestion.completed: uploadId={}, status={}", uploadId, status);

        uploadRepository.findByUploadId(uploadId).ifPresent(upload -> {
            String prevStatus = upload.getStatus().name();
            UploadStatus newStatus = "COMPLETED".equalsIgnoreCase(status)
                ? UploadStatus.COMPLETED : UploadStatus.FAILED;

            upload.setStatus(newStatus);
            if (newStatus == UploadStatus.COMPLETED) {
                upload.setCompletedAt(java.time.LocalDateTime.now());
            }
            uploadRepository.save(upload);

            uploadEventService.record(upload,
                newStatus == UploadStatus.COMPLETED ? "INGESTION_COMPLETED" : "INGESTION_FAILED",
                "INGEST_SERVICE", "system", "SYSTEM",
                prevStatus, newStatus.name(),
                "Ingestion " + status.toLowerCase(),
                payload, null);
        });
    }
}
