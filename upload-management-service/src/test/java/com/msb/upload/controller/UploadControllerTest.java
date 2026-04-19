package com.msb.upload.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msb.upload.config.SecurityConfig;
import com.msb.upload.dto.request.*;
import com.msb.upload.dto.response.*;
import com.msb.upload.exception.*;
import com.msb.upload.model.enums.SharePermission;
import com.msb.upload.model.enums.UploadStatus;
import com.msb.upload.security.JwtAuthenticationFilter;
import com.msb.upload.service.UploadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;

import java.time.LocalDateTime;
import java.util.List;

import static com.msb.upload.helper.TestJwtHelper.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UploadController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@MockBean(JpaMetamodelMappingContext.class)
@TestPropertySource(properties = {
    "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
})
@DisplayName("UploadController")
class UploadControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UploadService uploadService;

    // ── Shared test data ──────────────────────────────────────────────────────

    private static final String BASE = "/api/v1/uploads";

    private UploadResponse fakeUpload(UploadStatus status) {
        return UploadResponse.builder()
            .uploadId("UPL-2026-000001")
            .userId("user-1")
            .departmentId("FIN-001")
            .fileName("data.csv")
            .status(status)
            .build();
    }

    private PresignedUrlResponse fakePresigned() {
        return PresignedUrlResponse.builder()
            .uploadId("UPL-2026-000001")
            .presignedUrl("https://ecs.test/bucket/key?X-Amz-Signature=fake")
            .objectKey("uploads/2026/01/FIN-001/user-1/UPL-2026-000001/data.csv")
            .bucketName("test-bucket")
            .expiresAt(LocalDateTime.now().plusMinutes(15))
            .expiryMinutes(15)
            .build();
    }

    private MultipartInitiateResponse fakeMultipartResp() {
        return MultipartInitiateResponse.builder()
            .uploadId("UPL-2026-000001")
            .s3UploadId("s3-abc123")
            .objectKey("uploads/2026/01/FIN-001/user-1/UPL-2026-000001/data.csv")
            .bucketName("test-bucket")
            .fileSizeBytes(100L * 1024 * 1024)
            .partSizeBytes(10L * 1024 * 1024)
            .totalParts(10)
            .parts(List.of())
            .expiresAt(LocalDateTime.now().plusMinutes(15))
            .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. POST /presigned-url
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /presigned-url")
    class RequestPresignedUrl {

        private String validBody() throws Exception {
            PresignedUrlRequest req = new PresignedUrlRequest();
            req.setFileName("data.csv");
            req.setFileType("CSV");
            req.setDatasetType("FINANCIAL");
            return objectMapper.writeValueAsString(req);
        }

        @Test
        @DisplayName("No token → 401")
        void no_token_returns_401() throws Exception {
            mockMvc.perform(post(BASE + "/presigned-url")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody()))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Valid token + body → 201 with uploadId")
        void valid_returns_201() throws Exception {
            when(uploadService.requestPresignedUrl(any(), any())).thenReturn(fakePresigned());

            mockMvc.perform(post(BASE + "/presigned-url")
                    .header("Authorization", bearer(userToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uploadId").value("UPL-2026-000001"))
                .andExpect(jsonPath("$.data.presignedUrl").exists());
        }

        @Test
        @DisplayName("Missing required fields → 400")
        void missing_required_fields_returns_400() throws Exception {
            mockMvc.perform(post(BASE + "/presigned-url")
                    .header("Authorization", bearer(userToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Duplicate file → 409")
        void duplicate_file_returns_409() throws Exception {
            when(uploadService.requestPresignedUrl(any(), any()))
                .thenThrow(new DuplicateUploadException("UPL-OLD-001", "VALIDATED", "data.csv"));

            mockMvc.perform(post(BASE + "/presigned-url")
                    .header("Authorization", bearer(userToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.existingUploadId").value("UPL-OLD-001"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. POST /{uploadId}/confirm
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /{uploadId}/confirm")
    class ConfirmUpload {

        @Test
        @DisplayName("No token → 401")
        void no_token_returns_401() throws Exception {
            mockMvc.perform(post(BASE + "/UPL-001/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"etag\":\"abc123\"}"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Valid → 200 with upload response")
        void valid_returns_200() throws Exception {
            when(uploadService.confirmUpload(any(), any(), any()))
                .thenReturn(fakeUpload(UploadStatus.UPLOADED));

            mockMvc.perform(post(BASE + "/UPL-001/confirm")
                    .header("Authorization", bearer(userToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"etag\":\"abc123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UPLOADED"));
        }

        @Test
        @DisplayName("Missing etag (required) → 400")
        void missing_etag_returns_400() throws Exception {
            mockMvc.perform(post(BASE + "/UPL-001/confirm")
                    .header("Authorization", bearer(userToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("File not found in S3 → 409")
        void file_not_in_storage_returns_409() throws Exception {
            when(uploadService.confirmUpload(any(), any(), any()))
                .thenThrow(new InvalidUploadStateException("File not found in storage"));

            mockMvc.perform(post(BASE + "/UPL-001/confirm")
                    .header("Authorization", bearer(userToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"etag\":\"abc\"}"))
                .andExpect(status().isConflict());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. GET /{uploadId}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /{uploadId}")
    class GetUpload {

        @Test
        @DisplayName("No token → 401")
        void no_token_returns_401() throws Exception {
            mockMvc.perform(get(BASE + "/UPL-001"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Valid → 200 with upload data")
        void valid_returns_200() throws Exception {
            when(uploadService.getUpload(eq("UPL-001"), any()))
                .thenReturn(fakeUpload(UploadStatus.VALIDATED));

            mockMvc.perform(get(BASE + "/UPL-001")
                    .header("Authorization", bearer(userToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploadId").value("UPL-2026-000001"))
                .andExpect(jsonPath("$.data.status").value("VALIDATED"));
        }

        @Test
        @DisplayName("Not found → 404")
        void not_found_returns_404() throws Exception {
            when(uploadService.getUpload(any(), any()))
                .thenThrow(new UploadNotFoundException("UPL-GHOST"));

            mockMvc.perform(get(BASE + "/UPL-GHOST")
                    .header("Authorization", bearer(userToken())))
                .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. GET / (search)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET / (search)")
    class SearchUploads {

        @Test
        @DisplayName("No token → 401")
        void no_token_returns_401() throws Exception {
            mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Valid → 200 with page response")
        void valid_returns_200() throws Exception {
            UploadPageResponse page = UploadPageResponse.builder()
                .content(List.of(fakeUpload(UploadStatus.COMPLETED)))
                .page(0).size(20).totalElements(1).totalPages(1).last(true)
                .build();
            when(uploadService.searchUploads(any(), any())).thenReturn(page);

            mockMvc.perform(get(BASE)
                    .header("Authorization", bearer(userToken()))
                    .param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("COMPLETED"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. POST /{uploadId}/approve
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /{uploadId}/approve")
    class ApproveUpload {

        @Test
        @DisplayName("No token → 401")
        void no_token_returns_401() throws Exception {
            mockMvc.perform(post(BASE + "/UPL-001/approve"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("ROLE_USER → 403 (insufficient role)")
        void role_user_returns_403() throws Exception {
            mockMvc.perform(post(BASE + "/UPL-001/approve")
                    .header("Authorization", bearer(userToken())))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_DEPARTMENT_MANAGER → 200")
        void manager_can_approve() throws Exception {
            when(uploadService.approveUpload(any(), any(), any()))
                .thenReturn(fakeUpload(UploadStatus.APPROVED));

            mockMvc.perform(post(BASE + "/UPL-001/approve")
                    .header("Authorization", bearer(managerToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"comment\":\"OK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        }

        @Test
        @DisplayName("ROLE_ADMIN → 200")
        void admin_can_approve() throws Exception {
            when(uploadService.approveUpload(any(), any(), any()))
                .thenReturn(fakeUpload(UploadStatus.APPROVED));

            mockMvc.perform(post(BASE + "/UPL-001/approve")
                    .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Upload not in PENDING_APPROVAL → 409")
        void wrong_state_returns_409() throws Exception {
            when(uploadService.approveUpload(any(), any(), any()))
                .thenThrow(new InvalidUploadStateException("Must be PENDING_APPROVAL"));

            mockMvc.perform(post(BASE + "/UPL-001/approve")
                    .header("Authorization", bearer(managerToken())))
                .andExpect(status().isConflict());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. POST /{uploadId}/reject
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /{uploadId}/reject")
    class RejectUpload {

        @Test
        @DisplayName("No token → 401")
        void no_token_returns_401() throws Exception {
            mockMvc.perform(post(BASE + "/UPL-001/reject")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\":\"Invalid\"}"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("ROLE_USER → 403")
        void role_user_returns_403() throws Exception {
            mockMvc.perform(post(BASE + "/UPL-001/reject")
                    .header("Authorization", bearer(userToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\":\"Invalid\"}"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Missing reason → 400")
        void missing_reason_returns_400() throws Exception {
            mockMvc.perform(post(BASE + "/UPL-001/reject")
                    .header("Authorization", bearer(managerToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("ROLE_DEPARTMENT_MANAGER with reason → 200")
        void manager_can_reject() throws Exception {
            when(uploadService.rejectUpload(any(), any(), any()))
                .thenReturn(fakeUpload(UploadStatus.REJECTED));

            mockMvc.perform(post(BASE + "/UPL-001/reject")
                    .header("Authorization", bearer(managerToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\":\"Data quality issue\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. DELETE /{uploadId}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /{uploadId}")
    class DeleteUpload {

        @Test
        @DisplayName("No token → 401")
        void no_token_returns_401() throws Exception {
            mockMvc.perform(delete(BASE + "/UPL-001"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Valid → 200")
        void valid_returns_200() throws Exception {
            doNothing().when(uploadService).deleteUpload(any(), any());

            mockMvc.perform(delete(BASE + "/UPL-001")
                    .header("Authorization", bearer(userToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Other user's upload → 403")
        void other_users_upload_returns_403() throws Exception {
            doThrow(new org.springframework.security.access.AccessDeniedException("not your upload"))
                .when(uploadService).deleteUpload(any(), any());

            mockMvc.perform(delete(BASE + "/UPL-001")
                    .header("Authorization", bearer(userToken())))
                .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. POST /{uploadId}/share
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /{uploadId}/share")
    class ShareUpload {

        @Test
        @DisplayName("No token → 401")
        void no_token_returns_401() throws Exception {
            mockMvc.perform(post(BASE + "/UPL-001/share")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"sharedWithUserId\":\"user-2\",\"permission\":\"READ\"}"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Valid → 200")
        void valid_returns_200() throws Exception {
            when(uploadService.shareUpload(any(), any(), any()))
                .thenReturn(fakeUpload(UploadStatus.COMPLETED));

            ShareUploadRequest req = new ShareUploadRequest();
            req.setSharedWithUserId("user-2");
            req.setPermission(SharePermission.READ);

            mockMvc.perform(post(BASE + "/UPL-001/share")
                    .header("Authorization", bearer(userToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Missing permission (required) → 400")
        void missing_permission_returns_400() throws Exception {
            mockMvc.perform(post(BASE + "/UPL-001/share")
                    .header("Authorization", bearer(userToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"sharedWithUserId\":\"user-2\"}"))
                .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. GET /{uploadId}/validation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /{uploadId}/validation")
    class GetValidationResult {

        @Test
        @DisplayName("No token → 401")
        void no_token_returns_401() throws Exception {
            mockMvc.perform(get(BASE + "/UPL-001/validation"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Valid → 200 with validation data")
        void valid_returns_200() throws Exception {
            ValidationResultResponse vr = ValidationResultResponse.builder()
                .uploadId("UPL-2026-000001")
                .overallStatus("PASSED")
                .qualityScore(new java.math.BigDecimal("98.50"))
                .build();
            when(uploadService.getValidationResult("UPL-001")).thenReturn(vr);

            mockMvc.perform(get(BASE + "/UPL-001/validation")
                    .header("Authorization", bearer(userToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallStatus").value("PASSED"));
        }

        @Test
        @DisplayName("No validation result yet → 404")
        void no_result_returns_404() throws Exception {
            when(uploadService.getValidationResult(any()))
                .thenThrow(new UploadNotFoundException("No validation result"));

            mockMvc.perform(get(BASE + "/UPL-001/validation")
                    .header("Authorization", bearer(userToken())))
                .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 10. GET /{uploadId}/status
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /{uploadId}/status")
    class GetStatus {

        @Test
        @DisplayName("No token → 401")
        void no_token_returns_401() throws Exception {
            mockMvc.perform(get(BASE + "/UPL-001/status"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Valid → 200 with status string")
        void valid_returns_status_string() throws Exception {
            when(uploadService.getUpload(eq("UPL-001"), any()))
                .thenReturn(fakeUpload(UploadStatus.INGESTING));

            mockMvc.perform(get(BASE + "/UPL-001/status")
                    .header("Authorization", bearer(userToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("INGESTING"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 11. POST /multipart/initiate
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /multipart/initiate")
    class InitiateMultipart {

        private String validBody() throws Exception {
            MultipartInitiateRequest req = new MultipartInitiateRequest();
            req.setFileName("data.csv");
            req.setFileType("CSV");
            req.setFileSize(100L * 1024 * 1024);
            req.setDatasetType("FINANCIAL");
            req.setDepartmentId("FIN-001");
            return objectMapper.writeValueAsString(req);
        }

        @Test
        @DisplayName("No token → 401")
        void no_token_returns_401() throws Exception {
            mockMvc.perform(post(BASE + "/multipart/initiate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody()))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Valid → 201 with parts list")
        void valid_returns_201() throws Exception {
            when(uploadService.initiateMultipartUpload(any(), any()))
                .thenReturn(fakeMultipartResp());

            mockMvc.perform(post(BASE + "/multipart/initiate")
                    .header("Authorization", bearer(userToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.uploadId").value("UPL-2026-000001"))
                .andExpect(jsonPath("$.data.totalParts").value(10))
                .andExpect(jsonPath("$.data.s3UploadId").value("s3-abc123"));
        }

        @Test
        @DisplayName("fileSize missing (required) → 400")
        void missing_file_size_returns_400() throws Exception {
            MultipartInitiateRequest req = new MultipartInitiateRequest();
            req.setFileName("data.csv");
            req.setFileType("CSV");
            req.setDatasetType("FINANCIAL");
            // fileSize is missing → @NotNull violation

            mockMvc.perform(post(BASE + "/multipart/initiate")
                    .header("Authorization", bearer(userToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Permission denied for another dept → 403")
        void wrong_dept_returns_403() throws Exception {
            when(uploadService.initiateMultipartUpload(any(), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("wrong dept"));

            mockMvc.perform(post(BASE + "/multipart/initiate")
                    .header("Authorization", bearer(userToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody()))
                .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 12. POST /{uploadId}/multipart/complete
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /{uploadId}/multipart/complete")
    class CompleteMultipart {

        @Test
        @DisplayName("No token → 401")
        void no_token_returns_401() throws Exception {
            mockMvc.perform(post(BASE + "/UPL-001/multipart/complete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"parts\":[{\"partNumber\":1,\"etag\":\"abc\"}]}"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Valid → 200 with UPLOADED status")
        void valid_returns_200() throws Exception {
            when(uploadService.completeMultipartUpload(any(), any(), any()))
                .thenReturn(fakeUpload(UploadStatus.UPLOADED));

            mockMvc.perform(post(BASE + "/UPL-001/multipart/complete")
                    .header("Authorization", bearer(userToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"parts\":[{\"partNumber\":1,\"etag\":\"etag-abc\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UPLOADED"));
        }

        @Test
        @DisplayName("Empty parts list → 400")
        void empty_parts_returns_400() throws Exception {
            mockMvc.perform(post(BASE + "/UPL-001/multipart/complete")
                    .header("Authorization", bearer(userToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"parts\":[]}"))
                .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 13. DELETE /{uploadId}/multipart/abort
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /{uploadId}/multipart/abort")
    class AbortMultipart {

        @Test
        @DisplayName("No token → 401")
        void no_token_returns_401() throws Exception {
            mockMvc.perform(delete(BASE + "/UPL-001/multipart/abort"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Valid → 200")
        void valid_returns_200() throws Exception {
            doNothing().when(uploadService).abortMultipartUpload(any(), any());

            mockMvc.perform(delete(BASE + "/UPL-001/multipart/abort")
                    .header("Authorization", bearer(userToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Not owner → 403")
        void not_owner_returns_403() throws Exception {
            doThrow(new org.springframework.security.access.AccessDeniedException("not owner"))
                .when(uploadService).abortMultipartUpload(any(), any());

            mockMvc.perform(delete(BASE + "/UPL-001/multipart/abort")
                    .header("Authorization", bearer(userToken())))
                .andExpect(status().isForbidden());
        }
    }
}