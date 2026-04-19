package com.msb.upload.service;

import com.msb.upload.model.Upload;
import com.msb.upload.model.UploadEvent;
import com.msb.upload.repository.UploadEventRepository;
import com.msb.upload.service.impl.UploadEventServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UploadEventService")
class UploadEventServiceTest {

    @Mock UploadEventRepository uploadEventRepository;

    @InjectMocks UploadEventServiceImpl uploadEventService;

    // ── Factory ───────────────────────────────────────────────────────────────

    private Upload upload(String uploadId) {
        return Upload.builder().uploadId(uploadId).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // record
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Happy path → UploadEvent saved with all fields populated correctly")
    void record_saves_event_with_correct_fields() {
        Upload upload = upload("UPL-2026-000001");
        Map<String, Object> meta = Map.of("etag", "abc123", "fileSize", 1024L);

        uploadEventService.record(upload,
            "STATUS_CHANGE", "upload-service",
            "user-1", "ROLE_USER",
            "PENDING", "UPLOADED",
            "File confirmed", meta, "10.0.0.1");

        ArgumentCaptor<UploadEvent> captor = ArgumentCaptor.forClass(UploadEvent.class);
        verify(uploadEventRepository).save(captor.capture());

        UploadEvent saved = captor.getValue();
        assertThat(saved.getUpload()).isEqualTo(upload);
        assertThat(saved.getEventType()).isEqualTo("STATUS_CHANGE");
        assertThat(saved.getEventSource()).isEqualTo("upload-service");
        assertThat(saved.getActorId()).isEqualTo("user-1");
        assertThat(saved.getActorRole()).isEqualTo("ROLE_USER");
        assertThat(saved.getFromStatus()).isEqualTo("PENDING");
        assertThat(saved.getToStatus()).isEqualTo("UPLOADED");
        assertThat(saved.getMessage()).isEqualTo("File confirmed");
        assertThat(saved.getMetadata()).containsEntry("etag", "abc123");
        assertThat(saved.getIpAddress()).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("Repository throws exception → error is swallowed, no rethrow")
    void record_swallows_repository_exception() {
        when(uploadEventRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        assertThatNoException().isThrownBy(() ->
            uploadEventService.record(upload("UPL-001"),
                "STATUS_CHANGE", "svc", "user-1", "ROLE_USER",
                "PENDING", "UPLOADED", "msg", null, null));
    }

    @Test
    @DisplayName("Null metadata → saved without NPE")
    void record_with_null_metadata() {
        assertThatNoException().isThrownBy(() ->
            uploadEventService.record(upload("UPL-001"),
                "UPLOAD_INITIATED", "svc", "user-1", "ROLE_USER",
                null, null, null, null, null));

        verify(uploadEventRepository).save(any());
    }

    @Test
    @DisplayName("Null ipAddress and message → saved without NPE")
    void record_with_null_optional_string_fields() {
        assertThatNoException().isThrownBy(() ->
            uploadEventService.record(upload("UPL-001"),
                "PRESIGNED_URL_REQUESTED", "upload-service",
                "user-2", "ROLE_USER",
                null, "PENDING", null, null, null));

        ArgumentCaptor<UploadEvent> captor = ArgumentCaptor.forClass(UploadEvent.class);
        verify(uploadEventRepository).save(captor.capture());

        UploadEvent saved = captor.getValue();
        assertThat(saved.getActorId()).isEqualTo("user-2");
        assertThat(saved.getToStatus()).isEqualTo("PENDING");
        assertThat(saved.getIpAddress()).isNull();
        assertThat(saved.getMessage()).isNull();
    }

    @Test
    @DisplayName("Repository save called exactly once per record() invocation")
    void record_calls_save_exactly_once() {
        uploadEventService.record(upload("UPL-001"),
            "APPROVED", "svc", "mgr-1", "ROLE_DEPARTMENT_MANAGER",
            "PENDING_APPROVAL", "APPROVED", "Approved", null, "192.168.1.1");

        verify(uploadEventRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Multiple record() calls → each produces one save")
    void multiple_record_calls_each_produce_one_save() {
        Upload upload = upload("UPL-001");

        uploadEventService.record(upload, "TYPE_A", "svc", "u1", "ROLE_USER",
            null, "PENDING", null, null, null);
        uploadEventService.record(upload, "TYPE_B", "svc", "u1", "ROLE_USER",
            "PENDING", "UPLOADED", null, null, null);

        verify(uploadEventRepository, times(2)).save(any());
    }
}
