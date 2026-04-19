package com.msb.upload.service.impl;

import com.msb.upload.dto.request.CompletedPartInfo;
import com.msb.upload.exception.StorageException;
import com.msb.upload.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name:enterprise-uploads}")
    private String bucketName;

    // ── Presigned single-part URLs ────────────────────────────────────────────

    @Override
    public String generatePresignedPutUrl(String objectKey, int expiryMinutes) {
        try {
            return s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expiryMinutes))
                    .putObjectRequest(r -> r.bucket(bucketName).key(objectKey))
                    .build())
                .url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned PUT URL: {}", objectKey, e);
            throw new StorageException("Failed to generate upload URL", e);
        }
    }

    @Override
    public String generatePresignedGetUrl(String objectKey, int expiryMinutes) {
        try {
            return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expiryMinutes))
                    .getObjectRequest(r -> r.bucket(bucketName).key(objectKey))
                    .build())
                .url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned GET URL: {}", objectKey, e);
            throw new StorageException("Failed to generate download URL", e);
        }
    }

    // ── Object operations ─────────────────────────────────────────────────────

    @Override
    public boolean objectExists(String objectKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucketName).key(objectKey).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            throw new StorageException("Error checking object existence: " + objectKey, e);
        }
    }

    @Override
    public void deleteObject(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName).key(objectKey).build());
            log.info("Deleted S3 object: {}", objectKey);
        } catch (Exception e) {
            log.error("Failed to delete S3 object: {}", objectKey, e);
            throw new StorageException("Failed to delete object: " + objectKey, e);
        }
    }

    @Override
    public InputStream getObject(String bucket, String objectKey) {
        try {
            return s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket).key(objectKey).build());
        } catch (Exception e) {
            throw new StorageException("Failed to get object: " + objectKey, e);
        }
    }

    @Override
    public long getObjectSize(String objectKey) {
        try {
            return s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucketName).key(objectKey).build())
                .contentLength();
        } catch (Exception e) {
            throw new StorageException("Failed to get object size: " + objectKey, e);
        }
    }

    // ── Multipart upload ──────────────────────────────────────────────────────

    @Override
    public String initiateMultipartUpload(String objectKey) {
        try {
            String uploadId = s3Client.createMultipartUpload(
                CreateMultipartUploadRequest.builder()
                    .bucket(bucketName).key(objectKey).build())
                .uploadId();
            log.info("Initiated multipart upload: key={}, s3UploadId={}", objectKey, uploadId);
            return uploadId;
        } catch (Exception e) {
            log.error("Failed to initiate multipart upload: {}", objectKey, e);
            throw new StorageException("Failed to initiate multipart upload", e);
        }
    }

    @Override
    public String generatePresignedPartUrl(String objectKey, String s3UploadId,
                                           int partNumber, int expiryMinutes) {
        try {
            return s3Presigner.presignUploadPart(UploadPartPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expiryMinutes))
                    .uploadPartRequest(r -> r
                        .bucket(bucketName).key(objectKey)
                        .uploadId(s3UploadId).partNumber(partNumber))
                    .build())
                .url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned part URL: key={}, part={}", objectKey, partNumber, e);
            throw new StorageException("Failed to generate presigned part URL", e);
        }
    }

    @Override
    public void completeMultipartUpload(String objectKey, String s3UploadId,
                                        List<CompletedPartInfo> parts) {
        try {
            List<CompletedPart> completedParts = parts.stream()
                .map(p -> CompletedPart.builder()
                    .partNumber(p.getPartNumber())
                    .eTag(p.getEtag())
                    .build())
                .toList();

            s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(bucketName).key(objectKey).uploadId(s3UploadId)
                .multipartUpload(CompletedMultipartUpload.builder()
                    .parts(completedParts).build())
                .build());

            log.info("Completed multipart upload: key={}, s3UploadId={}, parts={}",
                objectKey, s3UploadId, parts.size());
        } catch (Exception e) {
            log.error("Failed to complete multipart upload: key={}, s3UploadId={}", objectKey, s3UploadId, e);
            throw new StorageException("Failed to complete multipart upload", e);
        }
    }

    @Override
    public void abortMultipartUpload(String objectKey, String s3UploadId) {
        try {
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                .bucket(bucketName).key(objectKey).uploadId(s3UploadId).build());
            log.info("Aborted multipart upload: key={}, s3UploadId={}", objectKey, s3UploadId);
        } catch (Exception e) {
            log.error("Failed to abort multipart upload: key={}, s3UploadId={}", objectKey, s3UploadId, e);
            throw new StorageException("Failed to abort multipart upload", e);
        }
    }
}