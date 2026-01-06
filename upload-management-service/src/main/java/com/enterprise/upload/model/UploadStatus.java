package com.enterprise.upload.model;

public enum UploadStatus {
    PENDING,           // Presigned URL generated
    UPLOADING,         // File is being uploaded
    UPLOADED,          // File uploaded to MinIO
    VALIDATING,        // Validation in progress
    VALIDATION_FAILED, // Validation failed
    VALID,            // Validation passed
    INGESTING,        // Ingestion in progress
    INGESTION_FAILED, // Ingestion failed
    COMPLETED,        // Successfully completed
    DELETED,          // Soft deleted
    FAILED            // General failure
}