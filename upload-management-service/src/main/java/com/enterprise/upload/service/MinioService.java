package com.enterprise.upload.service;

import java.io.InputStream;

public interface MinioService {
    String generatePresignedPutUrl(String objectKey, int expiryMinutes);
    String generatePresignedGetUrl(String objectKey, int expiryMinutes);
    boolean objectExists(String objectKey);
    void deleteObject(String objectKey);
    InputStream getObject(String bucketName, String objectKey);
    long getObjectSize(String objectKey);
}
