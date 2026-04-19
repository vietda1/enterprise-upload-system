package com.msb.upload.service;

import com.msb.upload.dto.request.*;
import com.msb.upload.dto.response.*;
import com.msb.upload.exception.*;
import com.msb.upload.mapper.UploadMapper;
import com.msb.upload.model.Upload;
import com.msb.upload.model.enums.SharePermission;
import com.msb.upload.model.enums.UploadStatus;
import com.msb.upload.repository.*;
import com.msb.upload.security.UserPrincipal;
import com.msb.upload.service.impl.UploadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UploadService")
class UploadServiceTest {

    @Mock UploadRepository                  uploadRepository;
    @Mock PresignedUrlLogRepository         presignedUrlLogRepository;
    @Mock ValidationDatasetResultRepository validationRepository;
    @Mock MinioService                      minioService;
    @Mock KafkaPublisherService             kafkaPublisherService;
    @Mock UploadEventService                uploadEventService;
    @Mock DatasetConfigRepository           datasetConfigRepository;
    @Mock UploadMapper                      uploadMapper;

    @InjectMocks UploadServiceImpl uploadService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(uploadService, "presignedUrlExpiryMinutes", 15);
        ReflectionTestUtils.setField(uploadService, "bucketName", "test-bucket");
    }

    // ── Factories ─────────────────────────────────────────────────────────────

    private UserPrincipal user(String userId, String deptId, String role) {
        return UserPrincipal.builder()
            .userId(userId).username(userId)
            .departmentId(deptId).roles(List.of(role)).build();
    }

    private UserPrincipal regularUser()  { return user("user-1", "FIN-001", "ROLE_USER"); }
    private UserPrincipal admin()        { return user("admin-1", "SYS", "ROLE_ADMIN"); }
    private UserPrincipal manager()      { return user("mgr-1", "FIN-001", "ROLE_DEPARTMENT_MANAGER"); }

    private Upload uploadInStatus(UploadStatus status) {
        return Upload.builder()
            .uploadId("UPL-2026-000001")
            .userId("user-1")
            .departmentId("FIN-001")
            .fileName("data.csv")
            .objectKey("uploads/2026/01/FIN-001/user-1/UPL-2026-000001/data.csv")
            .status(status)
            .presignedUrlExpiresAt(LocalDateTime.now().plusHours(1))
            .build();
    }

    private UploadResponse fakeResp(UploadStatus status) {
        return UploadResponse.builder()
            .uploadId("UPL-2026-000001")
            .userId("user-1")
            .departmentId("FIN-001")
            .status(status)
            .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. requestPresignedUrl
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("requestPresignedUrl")
    class RequestPresignedUrl {

        private PresignedUrlRequest validReq() {
            PresignedUrlRequest r = new PresignedUrlRequest();
            r.setFileName("data.csv"); r.setFileType("CSV");
            r.setDatasetType("FINANCIAL"); r.setDepartmentId("FIN-001");
            return r;
        }

        @BeforeEach
        void givenDatasetConfig() {
            lenient().when(datasetConfigRepository.findByCodeAndIsActiveTrue("FINANCIAL"))
                .thenReturn(Optional.of(com.msb.upload.model.DatasetConfig.builder()
                    .code("FINANCIAL").isActive(true).build()));
        }

        @Test
        @DisplayName("Happy path → presigned URL and PENDING upload created")
        void happy_path() {
            when(minioService.generatePresignedPutUrl(anyString(), anyInt()))
                .thenReturn("https://ecs.test/presigned");
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(presignedUrlLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PresignedUrlResponse resp = uploadService.requestPresignedUrl(validReq(), regularUser());

            assertThat(resp.getUploadId()).startsWith("UPL-");
            assertThat(resp.getPresignedUrl()).isEqualTo("https://ecs.test/presigned");
            assertThat(resp.getBucketName()).isEqualTo("test-bucket");
            verify(uploadRepository).save(argThat(u -> u.getStatus() == UploadStatus.PENDING));
        }

        @Test
        @DisplayName("Unknown datasetType → InvalidUploadStateException")
        void unknown_dataset_type_throws() {
            when(datasetConfigRepository.findByCodeAndIsActiveTrue("UNKNOWN"))
                .thenReturn(Optional.empty());
            PresignedUrlRequest req = validReq();
            req.setDatasetType("UNKNOWN");

            assertThatThrownBy(() -> uploadService.requestPresignedUrl(req, regularUser()))
                .isInstanceOf(InvalidUploadStateException.class)
                .hasMessageContaining("UNKNOWN");
        }

        @Test
        @DisplayName("Wrong department → AccessDeniedException")
        void wrong_dept_throws() {
            PresignedUrlRequest req = validReq();
            req.setDepartmentId("RISK-001"); // user belongs to FIN-001

            assertThatThrownBy(() -> uploadService.requestPresignedUrl(req, regularUser()))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Duplicate checksum → DuplicateUploadException")
        void duplicate_checksum_throws() {
            Upload existing = Upload.builder()
                .uploadId("UPL-OLD").status(UploadStatus.VALIDATED).fileName("data.csv").build();
            when(uploadRepository.findDuplicates(any(), any(), any(), any(), any()))
                .thenReturn(List.of(existing));

            PresignedUrlRequest req = validReq();
            req.setChecksum("sha256-abc");

            assertThatThrownBy(() -> uploadService.requestPresignedUrl(req, regularUser()))
                .isInstanceOf(DuplicateUploadException.class)
                .hasMessageContaining("UPL-OLD");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. confirmUpload
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("confirmUpload")
    class ConfirmUpload {

        @Test
        @DisplayName("Happy path → status UPLOADED, Kafka published")
        void happy_path() {
            Upload upload = uploadInStatus(UploadStatus.PENDING);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(minioService.objectExists(upload.getObjectKey())).thenReturn(true);
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(presignedUrlLogRepository.findByUploadIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
            when(uploadMapper.toResponse(any())).thenReturn(fakeResp(UploadStatus.UPLOADED));

            ConfirmUploadRequest req = new ConfirmUploadRequest();
            req.setEtag("etag-abc123");

            UploadResponse resp = uploadService.confirmUpload("UPL-001", req, regularUser());

            assertThat(resp.getStatus()).isEqualTo(UploadStatus.UPLOADED);
            verify(kafkaPublisherService).publishUploadCompleted(any());
        }

        @Test
        @DisplayName("Upload not in PENDING → InvalidUploadStateException")
        void non_pending_upload_throws() {
            Upload upload = uploadInStatus(UploadStatus.UPLOADED);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));

            assertThatThrownBy(() -> uploadService.confirmUpload("UPL-001",
                    new ConfirmUploadRequest(), regularUser()))
                .isInstanceOf(InvalidUploadStateException.class)
                .hasMessageContaining("UPLOADED");
        }

        @Test
        @DisplayName("File not in S3 → InvalidUploadStateException")
        void file_missing_from_s3_throws() {
            Upload upload = uploadInStatus(UploadStatus.PENDING);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(minioService.objectExists(upload.getObjectKey())).thenReturn(false);

            ConfirmUploadRequest req = new ConfirmUploadRequest();
            req.setEtag("etag");

            assertThatThrownBy(() -> uploadService.confirmUpload("UPL-001", req, regularUser()))
                .isInstanceOf(InvalidUploadStateException.class)
                .hasMessageContaining("not found in storage");
        }

        @Test
        @DisplayName("Other user tries to confirm → AccessDeniedException")
        void other_user_throws() {
            Upload upload = uploadInStatus(UploadStatus.PENDING);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));

            assertThatThrownBy(() -> uploadService.confirmUpload("UPL-001",
                    new ConfirmUploadRequest(), user("other-user", "FIN-001", "ROLE_USER")))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Upload not found → UploadNotFoundException")
        void not_found_throws() {
            when(uploadRepository.findByUploadId("GHOST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> uploadService.confirmUpload("GHOST",
                    new ConfirmUploadRequest(), regularUser()))
                .isInstanceOf(UploadNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. approveUpload
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("approveUpload")
    class ApproveUpload {

        @Test
        @DisplayName("PENDING_APPROVAL → APPROVED, approvedBy set")
        void happy_path() {
            Upload upload = uploadInStatus(UploadStatus.PENDING_APPROVAL);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(uploadMapper.toResponse(any())).thenReturn(fakeResp(UploadStatus.APPROVED));

            UploadResponse resp = uploadService.approveUpload("UPL-001",
                new ApproveUploadRequest(), manager());

            assertThat(resp.getStatus()).isEqualTo(UploadStatus.APPROVED);
            verify(uploadRepository).save(argThat(u ->
                u.getStatus() == UploadStatus.APPROVED && "mgr-1".equals(u.getApprovedBy())));
            verify(kafkaPublisherService).publishUploadStatusChanged(any(), eq("PENDING_APPROVAL"));
        }

        @Test
        @DisplayName("Not in PENDING_APPROVAL → InvalidUploadStateException")
        void wrong_state_throws() {
            Upload upload = uploadInStatus(UploadStatus.VALIDATED);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));

            assertThatThrownBy(() -> uploadService.approveUpload("UPL-001",
                    new ApproveUploadRequest(), manager()))
                .isInstanceOf(InvalidUploadStateException.class)
                .hasMessageContaining("PENDING_APPROVAL");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. rejectUpload
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectUpload")
    class RejectUpload {

        @Test
        @DisplayName("PENDING_APPROVAL → REJECTED with reason")
        void from_pending_approval() {
            Upload upload = uploadInStatus(UploadStatus.PENDING_APPROVAL);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(uploadMapper.toResponse(any())).thenReturn(fakeResp(UploadStatus.REJECTED));

            RejectUploadRequest req = new RejectUploadRequest();
            req.setReason("Data quality below threshold");

            UploadResponse resp = uploadService.rejectUpload("UPL-001", req, manager());

            assertThat(resp.getStatus()).isEqualTo(UploadStatus.REJECTED);
            verify(uploadRepository).save(argThat(u ->
                u.getStatus() == UploadStatus.REJECTED
                && "Data quality below threshold".equals(u.getRejectionReason())
                && "mgr-1".equals(u.getRejectedBy())));
        }

        @Test
        @DisplayName("VALIDATED → REJECTED (also allowed)")
        void from_validated() {
            Upload upload = uploadInStatus(UploadStatus.VALIDATED);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(uploadMapper.toResponse(any())).thenReturn(fakeResp(UploadStatus.REJECTED));

            RejectUploadRequest req = new RejectUploadRequest();
            req.setReason("Policy violation");

            assertThatNoException().isThrownBy(() ->
                uploadService.rejectUpload("UPL-001", req, manager()));
        }

        @Test
        @DisplayName("PENDING state → InvalidUploadStateException")
        void from_pending_throws() {
            Upload upload = uploadInStatus(UploadStatus.PENDING);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));

            RejectUploadRequest req = new RejectUploadRequest();
            req.setReason("reason");

            assertThatThrownBy(() -> uploadService.rejectUpload("UPL-001", req, manager()))
                .isInstanceOf(InvalidUploadStateException.class)
                .hasMessageContaining("PENDING");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. getUpload
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUpload")
    class GetUpload {

        @Test
        @DisplayName("Existing upload → mapped response")
        void existing_upload_returns_response() {
            Upload upload = uploadInStatus(UploadStatus.COMPLETED);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(uploadMapper.toResponse(upload)).thenReturn(fakeResp(UploadStatus.COMPLETED));

            UploadResponse resp = uploadService.getUpload("UPL-001", regularUser());

            assertThat(resp.getStatus()).isEqualTo(UploadStatus.COMPLETED);
        }

        @Test
        @DisplayName("Unknown uploadId → UploadNotFoundException")
        void unknown_upload_throws() {
            when(uploadRepository.findByUploadId("GHOST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> uploadService.getUpload("GHOST", regularUser()))
                .isInstanceOf(UploadNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. searchUploads
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("searchUploads")
    class SearchUploads {

        @Test
        @DisplayName("Non-admin with no filter → uses own departmentId")
        void non_admin_defaults_to_own_dept() {
            when(uploadRepository.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

            UploadSearchRequest req = new UploadSearchRequest();
            // no departmentId filter → should default to FIN-001

            uploadService.searchUploads(req, regularUser());

            verify(uploadRepository).search(
                isNull(), eq("FIN-001"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Admin with no filter → no dept restriction (null)")
        void admin_has_no_dept_restriction() {
            when(uploadRepository.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

            uploadService.searchUploads(new UploadSearchRequest(), admin());

            verify(uploadRepository).search(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Results mapped to page response with correct metadata")
        void results_mapped_correctly() {
            Upload u = uploadInStatus(UploadStatus.COMPLETED);
            when(uploadRepository.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(u)));
            when(uploadMapper.toResponse(u)).thenReturn(fakeResp(UploadStatus.COMPLETED));

            UploadPageResponse resp = uploadService.searchUploads(new UploadSearchRequest(), regularUser());

            assertThat(resp.getTotalElements()).isEqualTo(1);
            assertThat(resp.getContent()).hasSize(1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. deleteUpload
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteUpload")
    class DeleteUpload {

        @Test
        @DisplayName("Owner deletes → status DELETED, deletedAt set")
        void owner_can_delete() {
            Upload upload = uploadInStatus(UploadStatus.COMPLETED);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            uploadService.deleteUpload("UPL-001", regularUser());

            verify(uploadRepository).save(argThat(u ->
                u.getStatus() == UploadStatus.DELETED && u.getDeletedAt() != null));
        }

        @Test
        @DisplayName("Admin can delete any upload")
        void admin_can_delete_any() {
            Upload upload = Upload.builder()
                .uploadId("UPL-001").userId("other-user")
                .departmentId("RISK-001").status(UploadStatus.COMPLETED)
                .build();
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatNoException().isThrownBy(() ->
                uploadService.deleteUpload("UPL-001", admin()));
        }

        @Test
        @DisplayName("Non-owner tries to delete → AccessDeniedException")
        void non_owner_throws() {
            Upload upload = uploadInStatus(UploadStatus.COMPLETED); // userId=user-1
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));

            assertThatThrownBy(() ->
                uploadService.deleteUpload("UPL-001", user("other-user", "FIN-001", "ROLE_USER")))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Already soft-deleted → no-op, save not called again")
        void already_deleted_is_noop() {
            Upload upload = uploadInStatus(UploadStatus.DELETED);
            upload.setDeletedAt(LocalDateTime.now().minusDays(1));
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));

            uploadService.deleteUpload("UPL-001", regularUser());

            verify(uploadRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. shareUpload
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("shareUpload")
    class ShareUpload {

        @Test
        @DisplayName("Share with userId → UploadShare added")
        void share_with_user_id() {
            Upload upload = uploadInStatus(UploadStatus.COMPLETED);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(uploadMapper.toResponse(any())).thenReturn(fakeResp(UploadStatus.COMPLETED));

            ShareUploadRequest req = new ShareUploadRequest();
            req.setSharedWithUserId("user-2");
            req.setPermission(SharePermission.READ);

            assertThatNoException().isThrownBy(() ->
                uploadService.shareUpload("UPL-001", req, regularUser()));

            verify(uploadRepository).save(argThat(u -> !u.getShares().isEmpty()));
        }

        @Test
        @DisplayName("No target specified → InvalidUploadStateException")
        void no_target_throws() {
            Upload upload = uploadInStatus(UploadStatus.COMPLETED);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));

            ShareUploadRequest req = new ShareUploadRequest();
            req.setPermission(SharePermission.READ);
            // neither sharedWithUserId nor sharedWithDepartment

            assertThatThrownBy(() -> uploadService.shareUpload("UPL-001", req, regularUser()))
                .isInstanceOf(InvalidUploadStateException.class)
                .hasMessageContaining("sharedWithUserId");
        }

        @Test
        @DisplayName("Non-owner cannot share → AccessDeniedException")
        void non_owner_cannot_share() {
            Upload upload = uploadInStatus(UploadStatus.COMPLETED); // userId=user-1
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));

            ShareUploadRequest req = new ShareUploadRequest();
            req.setSharedWithUserId("user-3");
            req.setPermission(SharePermission.READ);

            assertThatThrownBy(() ->
                uploadService.shareUpload("UPL-001", req, user("other-user", "FIN-001", "ROLE_USER")))
                .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. getValidationResult
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getValidationResult")
    class GetValidationResult {

        @Test
        @DisplayName("Result exists → mapped response")
        void result_exists() {
            Upload upload = uploadInStatus(UploadStatus.VALIDATED);
            com.msb.upload.model.ValidationDatasetResult vr =
                new com.msb.upload.model.ValidationDatasetResult();
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(validationRepository.findFirstByUpload_UploadIdOrderByValidationRunDesc("UPL-001"))
                .thenReturn(Optional.of(vr));
            ValidationResultResponse fakeVr = ValidationResultResponse.builder()
                .uploadId("UPL-001").overallStatus("PASSED").build();
            when(uploadMapper.toValidationResponse(vr)).thenReturn(fakeVr);

            ValidationResultResponse resp = uploadService.getValidationResult("UPL-001");

            assertThat(resp.getOverallStatus()).isEqualTo("PASSED");
        }

        @Test
        @DisplayName("No result yet → UploadNotFoundException")
        void no_result_throws() {
            Upload upload = uploadInStatus(UploadStatus.UPLOADED);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(validationRepository.findFirstByUpload_UploadIdOrderByValidationRunDesc("UPL-001"))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> uploadService.getValidationResult("UPL-001"))
                .isInstanceOf(UploadNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 10. completeMultipartUpload
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("completeMultipartUpload")
    class CompleteMultipart {

        @Test
        @DisplayName("Happy path → UPLOADED, Kafka published")
        void happy_path() {
            Upload upload = uploadInStatus(UploadStatus.PENDING);
            upload.setMetadata(Map.of("s3UploadId", "s3-abc"));
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            doNothing().when(minioService).completeMultipartUpload(any(), any(), any());
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(uploadMapper.toResponse(any())).thenReturn(fakeResp(UploadStatus.UPLOADED));

            CompletedPartInfo part = new CompletedPartInfo();
            part.setPartNumber(1); part.setEtag("etag-1");
            CompleteMultipartRequest req = new CompleteMultipartRequest();
            req.setParts(List.of(part));

            UploadResponse resp = uploadService.completeMultipartUpload("UPL-001", req, regularUser());

            assertThat(resp.getStatus()).isEqualTo(UploadStatus.UPLOADED);
            verify(minioService).completeMultipartUpload(any(), eq("s3-abc"), any());
            verify(kafkaPublisherService).publishUploadCompleted(any());
        }

        @Test
        @DisplayName("Not PENDING → InvalidUploadStateException")
        void wrong_state_throws() {
            Upload upload = uploadInStatus(UploadStatus.UPLOADED);
            upload.setMetadata(Map.of("s3UploadId", "s3-abc"));
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));

            assertThatThrownBy(() -> uploadService.completeMultipartUpload(
                    "UPL-001", new CompleteMultipartRequest(), regularUser()))
                .isInstanceOf(InvalidUploadStateException.class)
                .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("No s3UploadId in metadata → InvalidUploadStateException")
        void missing_s3_upload_id_throws() {
            Upload upload = uploadInStatus(UploadStatus.PENDING);
            // metadata without s3UploadId
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));

            assertThatThrownBy(() -> uploadService.completeMultipartUpload(
                    "UPL-001", new CompleteMultipartRequest(), regularUser()))
                .isInstanceOf(InvalidUploadStateException.class)
                .hasMessageContaining("not initiated as multipart");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 11. abortMultipartUpload
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("abortMultipartUpload")
    class AbortMultipart {

        @Test
        @DisplayName("Happy path → S3 abort called, status DELETED")
        void happy_path() {
            Upload upload = uploadInStatus(UploadStatus.PENDING);
            upload.setMetadata(Map.of("s3UploadId", "s3-abc"));
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            doNothing().when(minioService).abortMultipartUpload(any(), any());
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            uploadService.abortMultipartUpload("UPL-001", regularUser());

            verify(minioService).abortMultipartUpload(any(), eq("s3-abc"));
            verify(uploadRepository).save(argThat(u ->
                u.getStatus() == UploadStatus.DELETED && u.getDeletedAt() != null));
        }

        @Test
        @DisplayName("No s3UploadId → S3 abort skipped, still marks DELETED")
        void no_s3_upload_id_skips_s3_abort() {
            Upload upload = uploadInStatus(UploadStatus.PENDING);
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));
            when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            uploadService.abortMultipartUpload("UPL-001", regularUser());

            verify(minioService, never()).abortMultipartUpload(any(), any());
            verify(uploadRepository).save(argThat(u -> u.getStatus() == UploadStatus.DELETED));
        }

        @Test
        @DisplayName("Non-owner → AccessDeniedException")
        void non_owner_throws() {
            Upload upload = uploadInStatus(UploadStatus.PENDING); // userId=user-1
            when(uploadRepository.findByUploadId("UPL-001")).thenReturn(Optional.of(upload));

            assertThatThrownBy(() ->
                uploadService.abortMultipartUpload("UPL-001", user("other", "FIN-001", "ROLE_USER")))
                .isInstanceOf(AccessDeniedException.class);
        }
    }
}