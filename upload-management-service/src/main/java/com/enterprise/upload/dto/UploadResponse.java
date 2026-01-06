package com.enterprise.upload.dto;

import com.enterprise.upload.model.AccessLevel;
import com.enterprise.upload.model.UploadStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class UploadResponse {
    private String id;
    private String userId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private UploadStatus status;
    private String department;
    private AccessLevel accessLevel;
    private String datasetType;
    private String targetDatabase;
    private Map<String, String> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime uploadedAt;
    private LocalDateTime validatedAt;
    private LocalDateTime ingestedAt;
    private String validationResult;
    private String ingestionResult;
    private String errorMessage;
}