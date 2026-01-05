package com.enterprise.upload.dto;

public class PresignedUrlResponse {
    private String uploadId;
    private String presignedUrl;
    private String objectKey;
    private int expirySeconds;
    
    public PresignedUrlResponse() {
    }
    
    public PresignedUrlResponse(String uploadId, String presignedUrl, String objectKey, int expirySeconds) {
        this.uploadId = uploadId;
        this.presignedUrl = presignedUrl;
        this.objectKey = objectKey;
        this.expirySeconds = expirySeconds;
    }
    
    public String getUploadId() {
        return uploadId;
    }
    
    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }
    
    public String getPresignedUrl() {
        return presignedUrl;
    }
    
    public void setPresignedUrl(String presignedUrl) {
        this.presignedUrl = presignedUrl;
    }
    
    public String getObjectKey() {
        return objectKey;
    }
    
    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }
    
    public int getExpirySeconds() {
        return expirySeconds;
    }
    
    public void setExpirySeconds(int expirySeconds) {
        this.expirySeconds = expirySeconds;
    }
}