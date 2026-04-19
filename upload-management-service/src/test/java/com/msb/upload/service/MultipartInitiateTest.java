package com.msb.upload.service;

import com.msb.upload.dto.request.MultipartInitiateRequest;
import com.msb.upload.dto.response.MultipartInitiateResponse;
import com.msb.upload.dto.response.PartUrlInfo;
import com.msb.upload.exception.DuplicateUploadException;
import com.msb.upload.exception.InvalidUploadStateException;
import com.msb.upload.mapper.UploadMapper;
import com.msb.upload.model.DatasetConfig;
import com.msb.upload.model.Upload;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho initiateMultipartUpload.
 * Không cần S3, PostgreSQL hay Spring context — toàn bộ dependencies được mock.
 */
@ExtendWith(MockitoExtension.class)
class MultipartInitiateTest {

    // ── Mocks (thay thế S3 + DB) ──────────────────────────────────────────────

    @Mock private UploadRepository              uploadRepository;
    @Mock private PresignedUrlLogRepository     presignedUrlLogRepository;
    @Mock private ValidationDatasetResultRepository validationRepository;
    @Mock private MinioService                  minioService;       // thay S3/ECS
    @Mock private KafkaPublisherService         kafkaPublisherService;
    @Mock private UploadEventService            uploadEventService;
    @Mock private DatasetConfigRepository       datasetConfigRepository; // thay PostgreSQL
    @Mock private UploadMapper                  uploadMapper;

    @InjectMocks
    private UploadServiceImpl uploadService;

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void injectConfigValues() {
        // Inject các @Value field (thường đến từ application.yml)
        ReflectionTestUtils.setField(uploadService, "presignedUrlExpiryMinutes", 15);
        ReflectionTestUtils.setField(uploadService, "bucketName", "test-bucket");
    }

    // ── Factory helpers ───────────────────────────────────────────────────────

    private UserPrincipal principal(String userId, String deptId, String role) {
        return UserPrincipal.builder()
            .userId(userId).username(userId)
            .departmentId(deptId)
            .roles(List.of(role))
            .build();
    }

    /** Tạo request mẫu: fileSizeMb MB chia thành partSizeMb MB/part */
    private MultipartInitiateRequest buildRequest(long fileSizeMb, int partSizeMb) {
        MultipartInitiateRequest req = new MultipartInitiateRequest();
        req.setFileName("data_2026.csv");
        req.setFileType("CSV");
        req.setMimeType("text/csv");
        req.setFileSize(fileSizeMb * 1024 * 1024);
        req.setDatasetType("FINANCIAL");
        req.setDepartmentId("FIN-001");
        req.setPartSizeMb(partSizeMb);
        return req;
    }

    /** Mock DB trả về dataset config hợp lệ */
    private void givenValidDatasetConfig() {
        DatasetConfig config = DatasetConfig.builder()
            .code("FINANCIAL").name("Financial Data").isActive(true).build();
        lenient().when(datasetConfigRepository.findByCodeAndIsActiveTrue("FINANCIAL"))
            .thenReturn(Optional.of(config));
    }

    /** Mock DB không tìm thấy file trùng */
    private void givenNoDuplicates() {
        lenient().when(uploadRepository.findDuplicates(any(), any(), any(), any(), any()))
            .thenReturn(List.of());
    }

    /** Mock S3/ECS trả về uploadId và presigned URLs giả */
    private void givenS3Returns(String s3UploadId) {
        when(minioService.initiateMultipartUpload(anyString()))
            .thenReturn(s3UploadId);
        when(minioService.generatePresignedPartUrl(anyString(), eq(s3UploadId), anyInt(), anyInt()))
            .thenAnswer(inv -> {
                int partNum = inv.getArgument(2);
                return "https://ecs.test/test-bucket/uploads/key?partNumber=" + partNum + "&X-Amz-Signature=fake";
            });
    }

    /** Mock DB save không làm gì — trả lại chính entity được save */
    private void givenSaveWorks() {
        when(uploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Happy path tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("1 GB / 10 MB → 103 parts, uploadId bắt đầu bằng UPL-")
        void file_1GB_partSize_10MB_returns_103_parts() {
            givenValidDatasetConfig();
            givenNoDuplicates();
            givenS3Returns("s3-upload-id-abc");
            givenSaveWorks();

            MultipartInitiateResponse resp = uploadService.initiateMultipartUpload(
                buildRequest(1024, 10),
                principal("user-1", "FIN-001", "ROLE_USER"));

            // uploadId được generate đúng format
            assertThat(resp.getUploadId()).startsWith("UPL-");
            assertThat(resp.getS3UploadId()).isEqualTo("s3-upload-id-abc");
            assertThat(resp.getBucketName()).isEqualTo("test-bucket");
            assertThat(resp.getFileSizeBytes()).isEqualTo(1024L * 1024 * 1024);
            assertThat(resp.getPartSizeBytes()).isEqualTo(10L * 1024 * 1024);

            // Số part = ceil(1024 / 10) = 103
            assertThat(resp.getTotalParts()).isEqualTo(103);
            assertThat(resp.getParts()).hasSize(103);
        }

        @Test
        @DisplayName("Part đầu tiên: startByte=0, endByte=10MB-1, URL chứa partNumber=1")
        void first_part_has_correct_byte_range_and_url() {
            givenValidDatasetConfig();
            givenNoDuplicates();
            givenS3Returns("s3-id");
            givenSaveWorks();

            MultipartInitiateResponse resp = uploadService.initiateMultipartUpload(
                buildRequest(1024, 10),
                principal("user-1", "FIN-001", "ROLE_USER"));

            PartUrlInfo part1 = resp.getParts().get(0);
            assertThat(part1.getPartNumber()).isEqualTo(1);
            assertThat(part1.getStartByte()).isEqualTo(0L);
            assertThat(part1.getEndByte()).isEqualTo(10L * 1024 * 1024 - 1);
            assertThat(part1.getPartSize()).isEqualTo(10L * 1024 * 1024);
            assertThat(part1.getUploadUrl()).contains("partNumber=1").contains("X-Amz-Signature");
        }

        @Test
        @DisplayName("Part cuối cùng (103): endByte = fileSize-1 (không bội số của partSize)")
        void last_part_ends_at_exact_file_boundary() {
            givenValidDatasetConfig();
            givenNoDuplicates();
            givenS3Returns("s3-id");
            givenSaveWorks();

            long fileSize = 1024L * 1024 * 1024; // 1GB
            MultipartInitiateResponse resp = uploadService.initiateMultipartUpload(
                buildRequest(1024, 10),
                principal("user-1", "FIN-001", "ROLE_USER"));

            PartUrlInfo lastPart = resp.getParts().get(102); // index 102 = part 103
            assertThat(lastPart.getPartNumber()).isEqualTo(103);
            assertThat(lastPart.getEndByte()).isEqualTo(fileSize - 1);
            // Part 103 nhỏ hơn 10MB (phần dư)
            assertThat(lastPart.getPartSize()).isLessThan(10L * 1024 * 1024);
        }

        @Test
        @DisplayName("File vừa đúng bội số của partSize: 1000MB / 10MB = 100 parts chẵn")
        void exact_multiple_file_returns_100_parts() {
            givenValidDatasetConfig();
            givenNoDuplicates();
            givenS3Returns("s3-id-exact");
            givenSaveWorks();

            MultipartInitiateResponse resp = uploadService.initiateMultipartUpload(
                buildRequest(1000, 10),
                principal("user-1", "FIN-001", "ROLE_USER"));

            assertThat(resp.getTotalParts()).isEqualTo(100);
            // Part cuối phải đúng bằng partSize
            PartUrlInfo last = resp.getParts().get(99);
            assertThat(last.getPartSize()).isEqualTo(10L * 1024 * 1024);
        }

        @Test
        @DisplayName("S3 initiateMultipartUpload + generatePresignedPartUrl được gọi đúng số lần")
        void s3_methods_called_correct_number_of_times() {
            givenValidDatasetConfig();
            givenNoDuplicates();
            givenS3Returns("s3-id-verify");
            givenSaveWorks();

            uploadService.initiateMultipartUpload(
                buildRequest(1024, 10),
                principal("user-1", "FIN-001", "ROLE_USER"));

            // initiateMultipartUpload chỉ gọi 1 lần
            verify(minioService, times(1)).initiateMultipartUpload(anyString());
            // generatePresignedPartUrl gọi 103 lần (1 lần/part)
            verify(minioService, times(103))
                .generatePresignedPartUrl(anyString(), eq("s3-id-verify"), anyInt(), eq(15));
            // DB save 1 lần
            verify(uploadRepository, times(1)).save(any(Upload.class));
        }

        @Test
        @DisplayName("s3UploadId được lưu vào metadata của Upload entity")
        void s3UploadId_stored_in_upload_metadata() {
            givenValidDatasetConfig();
            givenNoDuplicates();
            givenS3Returns("s3-stored-id");
            when(uploadRepository.save(any())).thenAnswer(inv -> {
                Upload saved = inv.getArgument(0);
                // Xác nhận metadata chứa s3UploadId
                assertThat(saved.getMetadata()).containsKey("s3UploadId");
                assertThat(saved.getMetadata().get("s3UploadId")).isEqualTo("s3-stored-id");
                assertThat(saved.getMetadata().get("totalParts")).isEqualTo(103);
                return saved;
            });

            uploadService.initiateMultipartUpload(
                buildRequest(1024, 10),
                principal("user-1", "FIN-001", "ROLE_USER"));
        }

        @Test
        @DisplayName("objectKey có format đúng: uploads/YYYY/MM/dept/userId/uploadId/filename")
        void objectKey_has_correct_format() {
            givenValidDatasetConfig();
            givenNoDuplicates();
            givenS3Returns("s3-id-key");
            when(uploadRepository.save(any())).thenAnswer(inv -> {
                Upload saved = inv.getArgument(0);
                assertThat(saved.getObjectKey())
                    .startsWith("uploads/")
                    .contains("FIN-001")
                    .contains("user-1")
                    .endsWith("data_2026.csv");
                return saved;
            });

            uploadService.initiateMultipartUpload(
                buildRequest(100, 10),
                principal("user-1", "FIN-001", "ROLE_USER"));
        }

        @Test
        @DisplayName("Upload status = PENDING sau khi tạo")
        void upload_status_is_pending() {
            givenValidDatasetConfig();
            givenNoDuplicates();
            givenS3Returns("s3-id");
            when(uploadRepository.save(any())).thenAnswer(inv -> {
                Upload saved = inv.getArgument(0);
                assertThat(saved.getStatus()).isEqualTo(UploadStatus.PENDING);
                return saved;
            });

            uploadService.initiateMultipartUpload(
                buildRequest(100, 10),
                principal("user-1", "FIN-001", "ROLE_USER"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Permission tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Permission check")
    class PermissionCheck {

        @Test
        @DisplayName("User upload cho đúng department của mình → OK")
        void user_uploads_for_own_department_ok() {
            givenValidDatasetConfig();
            givenNoDuplicates();
            givenS3Returns("s3-ok");
            givenSaveWorks();

            assertThatNoException().isThrownBy(() ->
                uploadService.initiateMultipartUpload(
                    buildRequest(100, 10),
                    principal("user-1", "FIN-001", "ROLE_USER")));
        }

        @Test
        @DisplayName("User upload cho department khác → 403 AccessDenied")
        void user_uploads_for_other_department_throws_403() {
            givenValidDatasetConfig();
            givenNoDuplicates();

            MultipartInitiateRequest req = buildRequest(100, 10);
            req.setDepartmentId("FIN-002"); // user thuộc FIN-001

            assertThatThrownBy(() ->
                uploadService.initiateMultipartUpload(req,
                    principal("user-1", "FIN-001", "ROLE_USER")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("FIN-002")
                .hasMessageContaining("FIN-001");
        }

        @Test
        @DisplayName("Admin upload cho bất kỳ department nào → OK")
        void admin_uploads_for_any_department_ok() {
            givenValidDatasetConfig();
            givenNoDuplicates();
            givenS3Returns("s3-admin");
            givenSaveWorks();

            MultipartInitiateRequest req = buildRequest(100, 10);
            req.setDepartmentId("FIN-999"); // department hoàn toàn khác

            assertThatNoException().isThrownBy(() ->
                uploadService.initiateMultipartUpload(req,
                    principal("admin-1", "SYS-DEPT", "ROLE_ADMIN")));
        }

        @Test
        @DisplayName("ROLE_DEPARTMENT_MANAGER chỉ upload được cho dept của mình")
        void dept_manager_restricted_to_own_dept() {
            givenValidDatasetConfig();
            givenNoDuplicates();

            MultipartInitiateRequest req = buildRequest(100, 10);
            req.setDepartmentId("RISK-001"); // manager thuộc FIN-001

            assertThatThrownBy(() ->
                uploadService.initiateMultipartUpload(req,
                    principal("mgr-1", "FIN-001", "ROLE_DEPARTMENT_MANAGER")))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Không truyền departmentId → dùng dept từ JWT principal")
        void no_departmentId_in_request_uses_principal_dept() {
            givenValidDatasetConfig();
            givenNoDuplicates();
            givenS3Returns("s3-default-dept");
            when(uploadRepository.save(any())).thenAnswer(inv -> {
                Upload saved = inv.getArgument(0);
                assertThat(saved.getDepartmentId()).isEqualTo("FIN-001"); // lấy từ principal
                return saved;
            });

            MultipartInitiateRequest req = buildRequest(100, 10);
            req.setDepartmentId(null); // không truyền dept

            assertThatNoException().isThrownBy(() ->
                uploadService.initiateMultipartUpload(req,
                    principal("user-1", "FIN-001", "ROLE_USER")));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Duplicate detection tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Duplicate file detection")
    class DuplicateDetection {

        @Test
        @DisplayName("Cùng dept + checksum + datasetType đã VALIDATED → 409 Conflict")
        void duplicate_checksum_same_dept_throws_409() {
            givenValidDatasetConfig();

            Upload existing = Upload.builder()
                .uploadId("UPL-2026-0001")
                .status(UploadStatus.VALIDATED)
                .fileName("data_2026.csv")
                .build();
            when(uploadRepository.findDuplicates(any(), any(), any(), any(), any()))
                .thenReturn(List.of(existing));

            MultipartInitiateRequest req = buildRequest(100, 10);
            req.setChecksum("sha256-abc123def456");

            assertThatThrownBy(() ->
                uploadService.initiateMultipartUpload(req,
                    principal("user-1", "FIN-001", "ROLE_USER")))
                .isInstanceOf(DuplicateUploadException.class)
                .hasMessageContaining("UPL-2026-0001")
                .hasMessageContaining("VALIDATED");
        }

        @Test
        @DisplayName("Không truyền checksum → bỏ qua kiểm tra dedup, không gọi DB")
        void no_checksum_skips_dedup_entirely() {
            givenValidDatasetConfig();
            givenS3Returns("s3-no-checksum");
            givenSaveWorks();

            MultipartInitiateRequest req = buildRequest(100, 10);
            req.setChecksum(null);

            assertThatNoException().isThrownBy(() ->
                uploadService.initiateMultipartUpload(req,
                    principal("user-1", "FIN-001", "ROLE_USER")));

            // findDuplicates không được gọi
            verify(uploadRepository, never()).findDuplicates(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Checksum rỗng → bỏ qua kiểm tra dedup")
        void blank_checksum_skips_dedup() {
            givenValidDatasetConfig();
            givenS3Returns("s3-blank");
            givenSaveWorks();

            MultipartInitiateRequest req = buildRequest(100, 10);
            req.setChecksum("   "); // blank

            assertThatNoException().isThrownBy(() ->
                uploadService.initiateMultipartUpload(req,
                    principal("user-1", "FIN-001", "ROLE_USER")));

            verify(uploadRepository, never()).findDuplicates(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("File trùng checksum nhưng dept KHÁC → cho phép upload")
        void duplicate_checksum_different_dept_is_allowed() {
            givenValidDatasetConfig();
            // findDuplicates trả về rỗng vì scope dedup là intra-department
            when(uploadRepository.findDuplicates(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
            givenS3Returns("s3-diff-dept");
            givenSaveWorks();

            MultipartInitiateRequest req = buildRequest(100, 10);
            req.setChecksum("sha256-same-file");
            req.setDepartmentId("RISK-001"); // dept khác FIN-001

            // Admin có thể upload cho RISK-001 với cùng checksum
            assertThatNoException().isThrownBy(() ->
                uploadService.initiateMultipartUpload(req,
                    principal("admin-1", "SYS-DEPT", "ROLE_ADMIN")));
        }

        @Test
        @DisplayName("File đã DELETED/EXPIRED → không bị block, cho upload lại")
        void deleted_or_expired_file_not_blocked() {
            givenValidDatasetConfig();
            // findDuplicates trả về rỗng — các status DELETED/EXPIRED đã bị exclude khỏi query
            when(uploadRepository.findDuplicates(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
            givenS3Returns("s3-re-upload");
            givenSaveWorks();

            MultipartInitiateRequest req = buildRequest(100, 10);
            req.setChecksum("sha256-old-deleted");

            assertThatNoException().isThrownBy(() ->
                uploadService.initiateMultipartUpload(req,
                    principal("user-1", "FIN-001", "ROLE_USER")));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Input validation")
    class InputValidation {

        @Test
        @DisplayName("DatasetType không tồn tại hoặc inactive → InvalidUploadStateException")
        void unknown_datasetType_throws_invalid_state() {
            when(datasetConfigRepository.findByCodeAndIsActiveTrue("UNKNOWN_TYPE"))
                .thenReturn(Optional.empty());

            MultipartInitiateRequest req = buildRequest(100, 10);
            req.setDatasetType("UNKNOWN_TYPE");

            assertThatThrownBy(() ->
                uploadService.initiateMultipartUpload(req,
                    principal("user-1", "FIN-001", "ROLE_USER")))
                .isInstanceOf(InvalidUploadStateException.class)
                .hasMessageContaining("UNKNOWN_TYPE");
        }

        @Test
        @DisplayName("checksumAlgorithm không truyền → mặc định SHA256")
        void null_checksumAlgorithm_defaults_to_SHA256() {
            givenValidDatasetConfig();
            givenNoDuplicates();
            givenS3Returns("s3-algo");
            when(uploadRepository.save(any())).thenAnswer(inv -> {
                Upload saved = inv.getArgument(0);
                assertThat(saved.getChecksumAlgorithm()).isEqualTo("SHA256");
                return saved;
            });

            MultipartInitiateRequest req = buildRequest(100, 10);
            req.setChecksumAlgorithm(null);

            uploadService.initiateMultipartUpload(req, principal("user-1", "FIN-001", "ROLE_USER"));
        }
    }
}