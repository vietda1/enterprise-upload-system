package com.msb.upload.service;

import com.msb.upload.dto.request.CompletedPartInfo;

import java.io.InputStream;
import java.util.List;

public interface MinioService {
    String generatePresignedPutUrl(String objectKey, int expiryMinutes);
    String generatePresignedGetUrl(String objectKey, int expiryMinutes);
    boolean objectExists(String objectKey);
    void deleteObject(String objectKey);
    InputStream getObject(String bucketName, String objectKey);
    long getObjectSize(String objectKey);

    // S3 Multipart Upload
    String initiateMultipartUpload(String objectKey);
    String generatePresignedPartUrl(String objectKey, String s3UploadId, int partNumber, int expiryMinutes);
    void completeMultipartUpload(String objectKey, String s3UploadId, List<CompletedPartInfo> parts);
    void abortMultipartUpload(String objectKey, String s3UploadId);
}