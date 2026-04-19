package com.msb.upload.service;

import com.msb.upload.model.KafkaEventLog;
import com.msb.upload.model.Upload;
import com.msb.upload.model.enums.UploadStatus;
import com.msb.upload.repository.KafkaEventLogRepository;
import com.msb.upload.service.impl.KafkaPublisherServiceImpl;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaPublisherService")
class KafkaPublisherServiceTest {

    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @Mock KafkaEventLogRepository       kafkaEventLogRepository;

    @InjectMocks KafkaPublisherServiceImpl kafkaPublisherService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(kafkaPublisherService,
            "uploadCompletedTopic", "upload.completed");
        ReflectionTestUtils.setField(kafkaPublisherService,
            "validationCompletedTopic", "validation.completed");
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    private Upload upload() {
        return Upload.builder()
            .uploadId("UPL-2026-000001")
            .userId("user-1")
            .objectKey("uploads/2026/01/FIN-001/user-1/UPL-001/data.csv")
            .bucketName("test-bucket")
            .datasetType("FINANCIAL")
            .targetDatabase("dw_prod")
            .targetTable("financial_data")
            .fileSize(1024L)
            .fileType("CSV")
            .status(UploadStatus.UPLOADED)
            .approvedBy("mgr-1")
            .build();
    }

    /** Stubs save() to set ID=1 on first call (simulates DB auto-increment). */
    private void givenSaveAssignsId() {
        AtomicLong seq = new AtomicLong(0L);
        doAnswer(inv -> {
            KafkaEventLog log = inv.getArgument(0);
            if (log.getId() == null) log.setId(seq.incrementAndGet());
            return log;
        }).when(kafkaEventLogRepository).save(any(KafkaEventLog.class));
    }

    /** Builds a successful CompletableFuture<SendResult>. */
    private CompletableFuture<SendResult<String, Object>> successFuture(int partition, long offset) {
        RecordMetadata meta = new RecordMetadata(
            new TopicPartition("upload.completed", partition), offset, 0, 0L, 0, 0);
        @SuppressWarnings("unchecked")
        SendResult<String, Object> result = mock(SendResult.class);
        when(result.getRecordMetadata()).thenReturn(meta);
        return CompletableFuture.completedFuture(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // publishUploadCompleted
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("publishUploadCompleted")
    class PublishUploadCompleted {

        @Test
        @DisplayName("Saves KafkaEventLog with status PENDING before send")
        void saves_event_log_with_pending_status() {
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(new CompletableFuture<>());

            kafkaPublisherService.publishUploadCompleted(upload());

            verify(kafkaEventLogRepository).save(argThat(log ->
                "PENDING".equals(log.getStatus())
                && "upload.completed".equals(log.getTopic())
                && "UPL-2026-000001".equals(log.getUploadId())));
        }

        @Test
        @DisplayName("Sends to 'upload.completed' topic with uploadId as key")
        void sends_to_correct_topic_with_upload_id_as_key() {
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(new CompletableFuture<>());

            kafkaPublisherService.publishUploadCompleted(upload());

            verify(kafkaTemplate).send(eq("upload.completed"), eq("UPL-2026-000001"), any());
        }

        @Test
        @DisplayName("Payload contains uploadId, userId, objectKey, bucketName, datasetType")
        void payload_contains_required_fields() {
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(new CompletableFuture<>());

            kafkaPublisherService.publishUploadCompleted(upload());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<java.util.Map<String, Object>> captor =
                ArgumentCaptor.forClass(java.util.Map.class);
            verify(kafkaTemplate).send(anyString(), anyString(), captor.capture());

            java.util.Map<String, Object> payload = captor.getValue();
            assertThat(payload).containsEntry("uploadId", "UPL-2026-000001");
            assertThat(payload).containsEntry("userId", "user-1");
            assertThat(payload).containsEntry("datasetType", "FINANCIAL");
            assertThat(payload).containsEntry("fileType", "CSV");
            assertThat(payload).containsKey("objectKey");
            assertThat(payload).containsKey("bucketName");
        }

        @Test
        @DisplayName("Null optional fields in Upload → replaced with empty string in payload")
        void null_optional_fields_replaced_with_empty_string() {
            Upload minimal = Upload.builder()
                .uploadId("UPL-2026-000002")
                .userId("user-2")
                .objectKey("key")
                .bucketName("bucket")
                // datasetType, targetDatabase, targetTable, fileType, fileSize all null
                .build();
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(new CompletableFuture<>());

            assertThatNoException().isThrownBy(() ->
                kafkaPublisherService.publishUploadCompleted(minimal));
        }

        @Test
        @DisplayName("Kafka send succeeds → log updated to SENT with partition and offset")
        void on_send_success_log_updated_to_sent() {
            givenSaveAssignsId();
            KafkaEventLog storedLog = KafkaEventLog.builder()
                .id(1L).status("PENDING").retryCount(0).build();
            when(kafkaEventLogRepository.findById(1L)).thenReturn(Optional.of(storedLog));
            var future = successFuture(2, 50L);
            when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

            kafkaPublisherService.publishUploadCompleted(upload());

            // Second save call should update status to SENT
            verify(kafkaEventLogRepository, atLeast(2)).save(any());
            assertThat(storedLog.getStatus()).isEqualTo("SENT");
            assertThat(storedLog.getPartition()).isEqualTo(2);
            assertThat(storedLog.getOffset()).isEqualTo(50L);
        }

        @Test
        @DisplayName("Kafka send fails → log updated to FAILED with error message")
        void on_send_failure_log_updated_to_failed() {
            givenSaveAssignsId();
            KafkaEventLog storedLog = KafkaEventLog.builder()
                .id(1L).status("PENDING").retryCount(0).build();
            when(kafkaEventLogRepository.findById(1L)).thenReturn(Optional.of(storedLog));
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka broker down")));

            kafkaPublisherService.publishUploadCompleted(upload());

            assertThat(storedLog.getStatus()).isEqualTo("FAILED");
            assertThat(storedLog.getErrorMessage()).contains("Kafka broker down");
            assertThat(storedLog.getRetryCount()).isEqualTo(1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // publishUploadStatusChanged
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("publishUploadStatusChanged")
    class PublishUploadStatusChanged {

        @Test
        @DisplayName("Saves KafkaEventLog with status PENDING before send")
        void saves_event_log_with_pending_status() {
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(new CompletableFuture<>());

            kafkaPublisherService.publishUploadStatusChanged(upload(), "PENDING_APPROVAL");

            verify(kafkaEventLogRepository).save(argThat(log ->
                "PENDING".equals(log.getStatus())
                && "UPL-2026-000001".equals(log.getUploadId())));
        }

        @Test
        @DisplayName("Payload contains previousStatus, newStatus, uploadId, approvedBy")
        void payload_contains_status_transition_fields() {
            Upload u = upload();
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(new CompletableFuture<>());

            kafkaPublisherService.publishUploadStatusChanged(u, "PENDING_APPROVAL");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<java.util.Map<String, Object>> captor =
                ArgumentCaptor.forClass(java.util.Map.class);
            verify(kafkaTemplate).send(anyString(), anyString(), captor.capture());

            java.util.Map<String, Object> payload = captor.getValue();
            assertThat(payload).containsEntry("uploadId", "UPL-2026-000001");
            assertThat(payload).containsEntry("previousStatus", "PENDING_APPROVAL");
            assertThat(payload).containsEntry("newStatus", "UPLOADED");
            assertThat(payload).containsEntry("approvedBy", "mgr-1");
        }

        @Test
        @DisplayName("Null approvedBy in Upload → replaced with empty string in payload")
        void null_approved_by_replaced_with_empty_string() {
            Upload u = Upload.builder()
                .uploadId("UPL-001").userId("u1")
                .objectKey("k").status(UploadStatus.REJECTED)
                .approvedBy(null)
                .build();
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(new CompletableFuture<>());

            assertThatNoException().isThrownBy(() ->
                kafkaPublisherService.publishUploadStatusChanged(u, "PENDING_APPROVAL"));
        }

        @Test
        @DisplayName("KafkaTemplate called once per publishUploadStatusChanged invocation")
        void kafka_template_called_once() {
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(new CompletableFuture<>());

            kafkaPublisherService.publishUploadStatusChanged(upload(), "VALIDATED");

            verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
        }
    }
}
