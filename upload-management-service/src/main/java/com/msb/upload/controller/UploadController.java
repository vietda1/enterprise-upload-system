package com.msb.upload.controller;

import com.msb.upload.dto.request.*;
import com.msb.upload.dto.response.*;
import com.msb.upload.security.UserPrincipal;
import com.msb.upload.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Tag(name = "Upload Management", description = "APIs for managing enterprise file uploads")
public class UploadController {

    private final UploadService uploadService;

    // ── 1. Request presigned URL ──────────────────────────────────────────────

    @PostMapping("/presigned-url")
    @Operation(summary = "Request a presigned URL to upload a file directly to S3/ECS")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> requestPresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Presigned URL generated",
                uploadService.requestPresignedUrl(request, principal)));
    }

    // ── 2. Confirm upload ─────────────────────────────────────────────────────

    @PostMapping("/{uploadId}/confirm")
    @Operation(summary = "Confirm file has been uploaded and trigger validation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UploadResponse>> confirmUpload(
            @PathVariable String uploadId,
            @Valid @RequestBody ConfirmUploadRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.ok("Upload confirmed, validation started",
            uploadService.confirmUpload(uploadId, request, principal)));
    }

    // ── 3. Get upload ─────────────────────────────────────────────────────────

    @GetMapping("/{uploadId}")
    @Operation(summary = "Get upload details by uploadId")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UploadResponse>> getUpload(
            @PathVariable String uploadId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.ok(uploadService.getUpload(uploadId, principal)));
    }

    // ── 4. Search / list uploads ──────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Search and list uploads with filters and pagination")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UploadPageResponse>> searchUploads(
            @ModelAttribute UploadSearchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.ok(uploadService.searchUploads(request, principal)));
    }

    // ── 5. Approve ────────────────────────────────────────────────────────────

    @PostMapping("/{uploadId}/approve")
    @Operation(summary = "Approve an upload (Dept Manager / Admin)")
    @PreAuthorize("hasAnyRole('ROLE_DEPARTMENT_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<UploadResponse>> approveUpload(
            @PathVariable String uploadId,
            @RequestBody(required = false) ApproveUploadRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ApproveUploadRequest req = request != null ? request : new ApproveUploadRequest();
        return ResponseEntity.ok(ApiResponse.ok("Upload approved",
            uploadService.approveUpload(uploadId, req, principal)));
    }

    // ── 6. Reject ─────────────────────────────────────────────────────────────

    @PostMapping("/{uploadId}/reject")
    @Operation(summary = "Reject an upload (Dept Manager / Admin)")
    @PreAuthorize("hasAnyRole('ROLE_DEPARTMENT_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<UploadResponse>> rejectUpload(
            @PathVariable String uploadId,
            @Valid @RequestBody RejectUploadRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.ok("Upload rejected",
            uploadService.rejectUpload(uploadId, request, principal)));
    }

    // ── 7. Delete ─────────────────────────────────────────────────────────────

    @DeleteMapping("/{uploadId}")
    @Operation(summary = "Soft-delete an upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteUpload(
            @PathVariable String uploadId,
            @AuthenticationPrincipal UserPrincipal principal) {

        uploadService.deleteUpload(uploadId, principal);
        return ResponseEntity.ok(ApiResponse.ok("Upload deleted", null));
    }

    // ── 8. Share ──────────────────────────────────────────────────────────────

    @PostMapping("/{uploadId}/share")
    @Operation(summary = "Share upload READ access with another user or department")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UploadResponse>> shareUpload(
            @PathVariable String uploadId,
            @Valid @RequestBody ShareUploadRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.ok("Upload shared",
            uploadService.shareUpload(uploadId, request, principal)));
    }

    // ── 9. Get validation result ──────────────────────────────────────────────

    @GetMapping("/{uploadId}/validation")
    @Operation(summary = "Get latest validation result for an upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ValidationResultResponse>> getValidationResult(
            @PathVariable String uploadId) {

        return ResponseEntity.ok(ApiResponse.ok(uploadService.getValidationResult(uploadId)));
    }

    // ── 10. Get upload status ─────────────────────────────────────────────────

    @GetMapping("/{uploadId}/status")
    @Operation(summary = "Get current status of an upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> getStatus(
            @PathVariable String uploadId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.ok(
            uploadService.getUpload(uploadId, principal).getStatus().name()));
    }

    // ── 11. Initiate multipart upload ─────────────────────────────────────────

    @PostMapping("/multipart/initiate")
    @Operation(summary = "Initiate a multipart upload — returns presigned PUT URLs for each part")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MultipartInitiateResponse>> initiateMultipartUpload(
            @Valid @RequestBody MultipartInitiateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Multipart upload initiated",
                uploadService.initiateMultipartUpload(request, principal)));
    }

    // ── 12. Complete multipart upload ─────────────────────────────────────────

    @PostMapping("/{uploadId}/multipart/complete")
    @Operation(summary = "Complete a multipart upload after all parts have been uploaded to S3/ECS")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UploadResponse>> completeMultipartUpload(
            @PathVariable String uploadId,
            @Valid @RequestBody CompleteMultipartRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.ok("Multipart upload completed",
            uploadService.completeMultipartUpload(uploadId, request, principal)));
    }

    // ── 13. Abort multipart upload ────────────────────────────────────────────

    @DeleteMapping("/{uploadId}/multipart/abort")
    @Operation(summary = "Abort an in-progress multipart upload and clean up S3/ECS resources")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> abortMultipartUpload(
            @PathVariable String uploadId,
            @AuthenticationPrincipal UserPrincipal principal) {

        uploadService.abortMultipartUpload(uploadId, principal);
        return ResponseEntity.ok(ApiResponse.ok("Multipart upload aborted", null));
    }
}