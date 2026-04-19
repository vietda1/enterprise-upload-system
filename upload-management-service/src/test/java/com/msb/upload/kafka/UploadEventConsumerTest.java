package com.msb.upload.kafka;

import com.msb.upload.model.Upload;
import com.msb.upload.model.enums.UploadStatus;
import com.msb.upload.repository.UploadRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UploadEventConsumer")
class UploadEventConsumerTest {

    @Mock UploadRepository uploadRepository;

    @InjectMocks UploadEventConsumer consumer;

    // ── Factory ───────────────────────────────────────────────────────────────

    private Upload upload(UploadStatus status) {
        return Upload.builder()
            .uploadId("UPL-2026-000001")
            .userId("user-1")
            .status(status)
            .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // handleValidationCompleted
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleValidationCompleted")
    class HandleValidationCompleted {

        @Test
        @DisplayName("status=VALID → upload set to VALIDATED and saved")
        void valid_status_sets_validated() {
            Upload upload = upload(UploadStatus.UPLOADED);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            consumer.handleValidationCompleted(Map.of("uploadId", "UPL-001", "status", "VALID"));

            verify(uploadRepository).save(argThat(u -> u.getStatus() == UploadStatus.VALIDATED));
        }

        @Test
        @DisplayName("status=INVALID → upload set to VALIDATION_FAILED and saved")
        void invalid_status_sets_validation_failed() {
            Upload upload = upload(UploadStatus.UPLOADED);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            consumer.handleValidationCompleted(Map.of("uploadId", "UPL-001", "status", "INVALID"));

            verify(uploadRepository).save(argThat(u -> u.getStatus() == UploadStatus.VALIDATION_FAILED));
        }

        @Test
        @DisplayName("Any non-VALID status → upload set to VALIDATION_FAILED")
        void non_valid_status_sets_validation_failed() {
            Upload upload = upload(UploadStatus.UPLOADED);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            consumer.handleValidationCompleted(Map.of("uploadId", "UPL-001", "status", "ERROR"));

            verify(uploadRepository).save(argThat(u -> u.getStatus() == UploadStatus.VALIDATION_FAILED));
        }

        @Test
        @DisplayName("Unknown uploadId → no-op, save never called")
        void unknown_upload_id_is_noop() {
            when(uploadRepository.findByUploadId("GHOST")).thenReturn(Optional.empty());

            consumer.handleValidationCompleted(Map.of("uploadId", "GHOST", "status", "VALID"));

            verify(uploadRepository, never()).save(any());
        }

        @Test
        @DisplayName("findByUploadId called with exact uploadId from event")
        void repository_called_with_exact_upload_id() {
            when(uploadRepository.findByUploadId("UPL-2026-000042")).thenReturn(Optional.empty());

            consumer.handleValidationCompleted(
                Map.of("uploadId", "UPL-2026-000042", "status", "VALID"));

            verify(uploadRepository).findByUploadId("UPL-2026-000042");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // handleIngestionCompleted
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleIngestionCompleted")
    class HandleIngestionCompleted {

        @Test
        @DisplayName("status=COMPLETED → upload set to COMPLETED and saved")
        void completed_status_sets_completed() {
            Upload upload = upload(UploadStatus.INGESTING);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            consumer.handleIngestionCompleted(Map.of("uploadId", "UPL-001", "status", "COMPLETED"));

            verify(uploadRepository).save(argThat(u -> u.getStatus() == UploadStatus.COMPLETED));
        }

        @Test
        @DisplayName("status=FAILED → upload set to FAILED and saved")
        void failed_status_sets_failed() {
            Upload upload = upload(UploadStatus.INGESTING);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            consumer.handleIngestionCompleted(Map.of("uploadId", "UPL-001", "status", "FAILED"));

            verify(uploadRepository).save(argThat(u -> u.getStatus() == UploadStatus.FAILED));
        }

        @Test
        @DisplayName("Any non-COMPLETED status → upload set to FAILED")
        void non_completed_status_sets_failed() {
            Upload upload = upload(UploadStatus.INGESTING);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            consumer.handleIngestionCompleted(Map.of("uploadId", "UPL-001", "status", "CANCELLED"));

            verify(uploadRepository).save(argThat(u -> u.getStatus() == UploadStatus.FAILED));
        }

        @Test
        @DisplayName("Unknown uploadId → no-op, save never called")
        void unknown_upload_id_is_noop() {
            when(uploadRepository.findByUploadId("GHOST")).thenReturn(Optional.empty());

            consumer.handleIngestionCompleted(Map.of("uploadId", "GHOST", "status", "COMPLETED"));

            verify(uploadRepository, never()).save(any());
        }

        @Test
        @DisplayName("findByUploadId called with exact uploadId from event")
        void repository_called_with_exact_upload_id() {
            when(uploadRepository.findByUploadId("UPL-2026-999")).thenReturn(Optional.empty());

            consumer.handleIngestionCompleted(
                Map.of("uploadId", "UPL-2026-999", "status", "COMPLETED"));

            verify(uploadRepository).findByUploadId("UPL-2026-999");
        }
    }
}
