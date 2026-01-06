package com.enterprise.upload.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "uploads", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_department", columnList = "department"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Upload {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false, length = 500)
    private String fileName;
    
    @Column(nullable = false, length = 100)
    private String fileType;
    
    @Column
    private Long fileSize;
    
    @Column(nullable = false, length = 1000)
    private String objectKey;
    
    @Column(nullable = false)
    private String bucketName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UploadStatus status;
    
    @Column(nullable = false, length = 100)
    private String department;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AccessLevel accessLevel;
    
    @Column(nullable = false, length = 100)
    private String datasetType;
    
    @Column(nullable = false, length = 500)
    private String targetDatabase;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "upload_metadata",
        joinColumns = @JoinColumn(name = "upload_id")
    )
    @MapKeyColumn(name = "meta_key", length = 255)
    @Column(name = "meta_value", columnDefinition = "TEXT")
    private Map<String, String> metadata = new HashMap<>();
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime uploadedAt;
    
    @Column
    private LocalDateTime validatedAt;
    
    @Column
    private LocalDateTime ingestedAt;
    
    @Column(columnDefinition = "TEXT")
    private String validationResult;
    
    @Column(columnDefinition = "TEXT")
    private String ingestionResult;
    
    @Column(length = 500)
    private String errorMessage;
    
    @Version
    private Long version;
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

    public String getBucketName() { return bucketName; }
    public void setBucketName(String bucketName) { this.bucketName = bucketName; }

    public UploadStatus getStatus() { return status; }
    public void setStatus(UploadStatus status) { this.status = status; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public AccessLevel getAccessLevel() { return accessLevel; }
    public void setAccessLevel(AccessLevel accessLevel) { this.accessLevel = accessLevel; }

    public String getDatasetType() { return datasetType; }
    public void setDatasetType(String datasetType) { this.datasetType = datasetType; }

    public String getTargetDatabase() { return targetDatabase; }
    public void setTargetDatabase(String targetDatabase) { this.targetDatabase = targetDatabase; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public LocalDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }

    public LocalDateTime getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(LocalDateTime ingestedAt) { this.ingestedAt = ingestedAt; }

    public String getValidationResult() { return validationResult; }
    public void setValidationResult(String validationResult) { this.validationResult = validationResult; }

    public String getIngestionResult() { return ingestionResult; }
    public void setIngestionResult(String ingestionResult) { this.ingestionResult = ingestionResult; } 
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }                          
}
