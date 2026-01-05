package com.enterprise.upload.model;

public enum UploadStatus {
    PENDING,
    UPLOADING,
    UPLOADED,
    VALIDATING,
    VALIDATION_FAILED,
    VALID,
    INGESTING,
    INGESTION_FAILED,
    COMPLETED,
    DELETED
}

