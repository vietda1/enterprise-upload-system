package com.msb.upload.service;

import com.msb.upload.dto.request.*;
import com.msb.upload.dto.response.*;
import com.msb.upload.security.UserPrincipal;

public interface UploadService {
    PresignedUrlResponse requestPresignedUrl(PresignedUrlRequest request, UserPrincipal principal);
    UploadResponse confirmUpload(String uploadId, ConfirmUploadRequest request, UserPrincipal principal);
    UploadResponse getUpload(String uploadId, UserPrincipal principal);
    UploadPageResponse searchUploads(UploadSearchRequest request, UserPrincipal principal);
    UploadResponse approveUpload(String uploadId, ApproveUploadRequest request, UserPrincipal principal);
    UploadResponse rejectUpload(String uploadId, RejectUploadRequest request, UserPrincipal principal);
    void deleteUpload(String uploadId, UserPrincipal principal);
    UploadResponse shareUpload(String uploadId, ShareUploadRequest request, UserPrincipal principal);
    ValidationResultResponse getValidationResult(String uploadId);

    // Multipart upload
    MultipartInitiateResponse initiateMultipartUpload(MultipartInitiateRequest request, UserPrincipal principal);
    UploadResponse completeMultipartUpload(String uploadId, CompleteMultipartRequest request, UserPrincipal principal);
    void abortMultipartUpload(String uploadId, UserPrincipal principal);
}