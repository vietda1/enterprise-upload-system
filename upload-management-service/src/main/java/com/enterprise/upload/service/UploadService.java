package com.enterprise.upload.service;

import com.enterprise.upload.dto.request.*;
import com.enterprise.upload.dto.response.*;

public interface UploadService {
    PresignedUrlResponse requestPresignedUrl(PresignedUrlRequest request, String userId);
    UploadResponse confirmUpload(String uploadId, ConfirmUploadRequest request, String userId);
    UploadResponse getUpload(String uploadId, String userId);
    UploadPageResponse searchUploads(UploadSearchRequest request, String userId);
    UploadResponse approveUpload(String uploadId, ApproveUploadRequest request, String approverId);
    UploadResponse rejectUpload(String uploadId, RejectUploadRequest request, String rejecterId);
    void deleteUpload(String uploadId, String userId);
    UploadResponse shareUpload(String uploadId, ShareUploadRequest request, String userId);
    ValidationResultResponse getValidationResult(String uploadId);
}
