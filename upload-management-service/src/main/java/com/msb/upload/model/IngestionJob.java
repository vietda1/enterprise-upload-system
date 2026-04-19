package com.msb.upload.model;

import com.msb.upload.model.enums.IngestionStatus;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Celery job theo dõi quá trình ingest data vào target database.
 * Hỗ trợ retry tối đa {@code maxRetries} lần.
 *
 * <p>Mapping: {@code upload_db.ingestion_jobs}
 */
@Entity
@Table(
    schema = "upload_db",
    name   = "ingestion_jobs",
    uniqueConstraints = @UniqueConstraint(
        name        = "ingestion_jobs_job_id_key",
        columnNames = "job_id"
    )
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Business key của job, e.g. JOB-2026-001 */
    @Column(name = "job_id", nullable = false, length = 100, unique = true)
    private String jobId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name                 = "upload_id",
        referencedColumnName = "upload_id",
        nullable             = false,
        foreignKey           = @ForeignKey(name = "ingestion_jobs_upload_id_fkey")
    )
    private Upload upload;

    /** Task ID do Celery gán, dùng để revoke hoặc check trạng thái */
    @Column(name = "celery_task_id", length = 200)
    private String celeryTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private IngestionStatus status = IngestionStatus.PENDING;

    // ─── Target info ─────────────────────────────────────────────────────────

    @Column(name = "target_database", length = 500)
    private String targetDatabase;

    @Column(name = "target_table", length = 200)
    private String targetTable;

    @Column(name = "target_db_type", length = 50)
    private String targetDbType;

    // ─── Progress counters ───────────────────────────────────────────────────

    @Column(name = "rows_processed")
    @Builder.Default
    private Long rowsProcessed = 0L;

    @Column(name = "rows_inserted")
    @Builder.Default
    private Long rowsInserted = 0L;

    @Column(name = "rows_failed")
    @Builder.Default
    private Long rowsFailed = 0L;

    @Column(name = "rows_skipped")
    @Builder.Default
    private Long rowsSkipped = 0L;

    // ─── Retry config ────────────────────────────────────────────────────────

    @Column(name = "batch_size")
    @Builder.Default
    private Integer batchSize = 1000;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    /**
     * Log từng bước thực thi dạng JSONB.
     * e.g. [{"timestamp":"...","message":"Inserted batch 1-50 (50000 rows)"}]
     */
    @Type(JsonBinaryType.class)
    @Column(name = "execution_log", columnDefinition = "jsonb")
    private List<Map<String, Object>> executionLog;

    // ─── Timestamps ──────────────────────────────────────────────────────────

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public boolean canRetry() {
        return retryCount < maxRetries && status == IngestionStatus.FAILED;
    }

    public boolean isRunning() {
        return status == IngestionStatus.RUNNING || status == IngestionStatus.RETRYING;
    }

    public long getDurationSeconds() {
        if (startedAt == null || completedAt == null) return 0;
        return java.time.Duration.between(startedAt, completedAt).getSeconds();
    }
}
