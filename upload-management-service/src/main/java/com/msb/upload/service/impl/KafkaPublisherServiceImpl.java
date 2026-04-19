package com.msb.upload.service.impl;

import com.msb.upload.model.KafkaEventLog;
import com.msb.upload.model.Upload;
import com.msb.upload.repository.KafkaEventLogRepository;
import com.msb.upload.service.KafkaPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPublisherServiceImpl implements KafkaPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaEventLogRepository kafkaEventLogRepository;

    @Value("${kafka.topics.upload-completed:upload.completed}")
    private String uploadCompletedTopic;

    @Value("${kafka.topics.validation-completed:validation.completed}")
    private String validationCompletedTopic;

    @Override
    public void publishUploadCompleted(Upload upload) {
        Map<String, Object> payload = Map.of(
            "uploadId",      upload.getUploadId(),
            "userId",        upload.getUserId(),
            "objectKey",     upload.getObjectKey(),
            "bucketName",    upload.getBucketName(),
            "datasetType",   upload.getDatasetType() != null ? upload.getDatasetType() : "",
            "targetDatabase", upload.getTargetDatabase() != null ? upload.getTargetDatabase() : "",
            "targetTable",   upload.getTargetTable() != null ? upload.getTargetTable() : "",
            "fileSize",      upload.getFileSize() != null ? upload.getFileSize() : 0L,
            "fileType",      upload.getFileType() != null ? upload.getFileType() : ""
        );
        publish(uploadCompletedTopic, upload.getUploadId(), payload, upload.getUploadId());
    }

    @Override
    public void publishUploadStatusChanged(Upload upload, String previousStatus) {
        Map<String, Object> payload = Map.of(
            "uploadId",      upload.getUploadId(),
            "previousStatus", previousStatus,
            "newStatus",     upload.getStatus().name(),
            "approvedBy",    upload.getApprovedBy() != null ? upload.getApprovedBy() : "",
            "objectKey",     upload.getObjectKey(),
            "datasetType",   upload.getDatasetType() != null ? upload.getDatasetType() : ""
        );
        publish(uploadCompletedTopic, upload.getUploadId(), payload, upload.getUploadId());
    }

    private void publish(String topic, String key, Map<String, Object> payload, String uploadId) {
        KafkaEventLog eventLog = KafkaEventLog.builder()
            .uploadId(uploadId)
            .topic(topic)
            .key(key)
            .payload(payload)
            .status("PENDING")
            .build();
        kafkaEventLogRepository.save(eventLog);

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, payload);
        final Long logId = eventLog.getId();

        future.whenComplete((result, ex) -> {
            kafkaEventLogRepository.findById(logId).ifPresent(log -> {
                if (ex == null) {
                    log.setStatus("SENT");
                    log.setPartition(result.getRecordMetadata().partition());
                    log.setOffset(result.getRecordMetadata().offset());
                } else {
                    log.setStatus("FAILED");
                    log.setErrorMessage(ex.getMessage());
                    log.setRetryCount(log.getRetryCount() + 1);
                }
                kafkaEventLogRepository.save(log);
            });
        });
        log.info("Published to Kafka: topic={}, uploadId={}", topic, uploadId);
    }
}
