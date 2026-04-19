package com.msb.upload.model;

import com.msb.upload.model.enums.UploadStatus;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entity chính đại diện cho một lần upload file.
 *
 * <p>Mapping: {@code upload_db.uploads}
 *
 * <p>Lưu ý: bảng có 2 trigger DB-level:
 * <ul>
 *   <li>{@code trg_uploads_updated_at}         – tự cập nhật {@code updated_at}</li>
 *   <li>{@code trg_log_upload_status_change}   – ghi log vào {@code upload_history}</li>
 * </ul>
 */
@Entity
@Table(
    schema = "upload_db",
    name   = "uploads",
    uniqueConstraints = @UniqueConstraint(
        name        = "uploads_upload_id_key",
        columnNames = "upload_id"
    )
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Upload {

    // ─── Primary Key ─────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    // ─── Identity ────────────────────────────────────────────────────────────

    /**
     * Business key hiển thị, dạng UPL-YYYY-NNNN.
     * Unique, immutable sau khi tạo.
     */
    @Column(name = "upload_id", nullable = false, length = 100, unique = true, updatable = false)
    private String uploadId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "department_id", length = 50)
    private String departmentId;

    // ─── File info ───────────────────────────────────────────────────────────

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "original_file_name", length = 500)
    private String originalFileName;

    /** Kích thước file tính bằng bytes */
    @Column(name = "file_size")
    private Long fileSize;

    /** Extension: CSV, JSON, XML, PARQUET... */
    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "mime_type", length = 200)
    private String mimeType;

    // ─── Object Storage ──────────────────────────────────────────────────────

    /** Object key trong MinIO/S3, e.g. uploads/2026/01/DEPT/user/UPL-xxx/file.csv */
    @Column(name = "object_key", nullable = false, length = 1000)
    private String objectKey;

    @Column(name = "bucket_name", length = 200)
    @Builder.Default
    private String bucketName = "enterprise-uploads";

    /** ETag trả về từ MinIO/S3 sau khi upload thành công */
    @Column(name = "etag", length = 200)
    private String etag;

    /** Checksum do frontend tính trước khi upload (SHA-256 hoặc MD5). Dùng để dedup. */
    @Column(name = "checksum", length = 128)
    private String checksum;

    /** SHA256 | MD5 */
    @Column(name = "checksum_algorithm", length = 20)
    private String checksumAlgorithm;

    // ─── Dataset / Target ────────────────────────────────────────────────────

    /** Mã loại dataset, tham chiếu logic đến DatasetConfig.code */
    @Column(name = "dataset_type", length = 50)
    private String datasetType;

    @Column(name = "target_database", length = 500)
    private String targetDatabase;

    @Column(name = "target_table", length = 200)
    private String targetTable;

    // ─── User-supplied info ───────────────────────────────────────────────────

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** Tags dạng JSONB tự do, e.g. {"month":"2026-01","batch":1} */
    @Type(JsonBinaryType.class)
    @Column(name = "tags", columnDefinition = "jsonb")
    private Map<String, Object> tags;

    /** Metadata bổ sung dạng JSONB */
    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    // ─── Status & Workflow ───────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private UploadStatus status = UploadStatus.PENDING;

    // Rejection
    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    // Approval
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // Presigned URL
    @Column(name = "presigned_url_expires_at")
    private LocalDateTime presignedUrlExpiresAt;

    // ─── Timestamps ──────────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Cập nhật tự động qua DB trigger {@code trg_uploads_updated_at}.
     * Không dùng @PreUpdate để tránh conflict với trigger.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Soft-delete timestamp */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // ─── Relationships ────────────────────────────────────────────────────────

    @OneToMany(
        mappedBy      = "upload",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    @OrderBy("created_at ASC")
    @Builder.Default
    private List<UploadEvent> events = new ArrayList<>();

    @OneToMany(
        mappedBy      = "upload",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    @OrderBy("changed_at DESC")
    @Builder.Default
    private List<UploadHistory> history = new ArrayList<>();

    @OneToMany(
        mappedBy      = "upload",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    @Builder.Default
    private List<UploadMetadata> metadataEntries = new ArrayList<>();

    @OneToMany(
        mappedBy      = "upload",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    @Builder.Default
    private List<UploadTag> uploadTags = new ArrayList<>();

    @OneToMany(
        mappedBy      = "upload",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    @Builder.Default
    private List<UploadShare> shares = new ArrayList<>();

    @OneToMany(
        mappedBy = "upload",
        fetch    = FetchType.LAZY
    )
    @OrderBy("created_at DESC")
    @Builder.Default
    private List<IngestionJob> ingestionJobs = new ArrayList<>();

    @OneToMany(
        mappedBy = "upload",
        fetch    = FetchType.LAZY
    )
    @OrderBy("created_at DESC")
    @Builder.Default
    private List<ValidationDatasetResult> validationResults = new ArrayList<>();

    // ─── Business helpers ────────────────────────────────────────────────────

    public boolean isExpiredPresignedUrl() {
        return presignedUrlExpiresAt != null
            && LocalDateTime.now().isAfter(presignedUrlExpiresAt);
    }

    public boolean isSoftDeleted() {
        return deletedAt != null;
    }

    public boolean isTerminalStatus() {
        return status == UploadStatus.COMPLETED
            || status == UploadStatus.FAILED
            || status == UploadStatus.REJECTED
            || status == UploadStatus.VALIDATION_FAILED
            || status == UploadStatus.EXPIRED
            || status == UploadStatus.DELETED;
    }

    public void addEvent(UploadEvent event) {
        events.add(event);
        event.setUpload(this);
    }

    public void addTag(UploadTag tag) {
        uploadTags.add(tag);
        tag.setUpload(this);
    }

    public void addMetadata(UploadMetadata meta) {
        metadataEntries.add(meta);
        meta.setUpload(this);
    }

    public void addShare(UploadShare share) {
        shares.add(share);
        share.setUpload(this);
    }
}
