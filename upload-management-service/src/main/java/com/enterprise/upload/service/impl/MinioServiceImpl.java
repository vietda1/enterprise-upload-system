package com.enterprise.upload.service.impl;

import com.enterprise.upload.exception.StorageException;
import com.enterprise.upload.service.MinioService;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name:enterprise-uploads}")
    private String bucketName;

    @Override
    public String generatePresignedPutUrl(String objectKey, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(bucketName)
                .object(objectKey)
                .expiry(expiryMinutes, TimeUnit.MINUTES)
                .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned PUT URL for: {}", objectKey, e);
            throw new StorageException("Failed to generate upload URL", e);
        }
    }

    @Override
    public String generatePresignedGetUrl(String objectKey, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(objectKey)
                .expiry(expiryMinutes, TimeUnit.MINUTES)
                .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned GET URL for: {}", objectKey, e);
            throw new StorageException("Failed to generate download URL", e);
        }
    }

    @Override
    public boolean objectExists(String objectKey) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .build());
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) return false;
            throw new StorageException("Error checking object existence: " + objectKey, e);
        } catch (Exception e) {
            throw new StorageException("Error checking object existence: " + objectKey, e);
        }
    }

    @Override
    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .build());
            log.info("Deleted object from MinIO: {}", objectKey);
        } catch (Exception e) {
            log.error("Failed to delete object: {}", objectKey, e);
            throw new StorageException("Failed to delete object: " + objectKey, e);
        }
    }

    @Override
    public InputStream getObject(String bucket, String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build());
        } catch (Exception e) {
            throw new StorageException("Failed to get object: " + objectKey, e);
        }
    }

    @Override
    public long getObjectSize(String objectKey) {
        try {
            return minioClient.statObject(StatObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .build()).size();
        } catch (Exception e) {
            throw new StorageException("Failed to get object size: " + objectKey, e);
        }
    }
}
