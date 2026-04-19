package com.msb.upload.dto.response;

import com.msb.upload.model.enums.UploadStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data @Builder
public class UploadResponse {
    private String uploadId;
    private String userId;
    private String departmentId;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String fileType;
    private String mimeType;
    private String objectKey;
    private String bucketName;
    private String datasetType;
    private String targetDatabase;
    private String targetTable;
    private String description;
    private Map<String, Object> tags;
    private UploadStatus status;
    private String rejectionReason;
    private String rejectedBy;
    private LocalDateTime rejectedAt;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime presignedUrlExpiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private List<String> uploadTags;
    private ValidationSummary latestValidation;
    private IngestionSummary latestIngestion;

    @Data @Builder
    public static class ValidationSummary {
        private Long id;
        private String overallStatus;
        private Double qualityScore;
        private Long totalRows;
        private Long totalValidRows;
        private Long totalInvalidRows;
        private Integer blockerCount;
        private LocalDateTime completedAt;
    }

    @Data @Builder
    public static class IngestionSummary {
        private String jobId;
        private String status;
        private Long rowsInserted;
        private Long rowsFailed;
        private LocalDateTime completedAt;
    }
}
