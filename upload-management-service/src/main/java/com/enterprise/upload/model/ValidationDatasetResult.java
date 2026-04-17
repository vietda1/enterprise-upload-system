package com.enterprise.upload.model;

import com.enterprise.upload.model.enums.ValidationStatus;
import com.enterprise.upload.model.enums.VirusScanStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả validation tổng hợp cho một lần upload (tất cả tables).
 * Một upload có thể validate nhiều lần ({@code validation_run}).
 *
 * <p>Mapping: {@code upload_db.validation_dataset_results}
 */
@Entity
@Table(
    schema = "upload_db",
    name   = "validation_dataset_results"
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationDatasetResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name                 = "upload_id",
        referencedColumnName = "upload_id",
        nullable             = false,
        foreignKey           = @ForeignKey(name = "fk_upload_id")
    )
    private Upload upload;

    /** Lần validate thứ N (bắt đầu từ 1, tăng khi validate lại) */
    @Column(name = "validation_run")
    @Builder.Default
    private Integer validationRun = 1;

    // ─── Overall status ───────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false, length = 30)
    @Builder.Default
    private ValidationStatus overallStatus = ValidationStatus.PENDING;

    /** Điểm chất lượng tổng hợp 0–100 */
    @Column(name = "quality_score", precision = 5, scale = 2)
    private BigDecimal qualityScore;

    // ─── Virus scan ───────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "virus_scan_status", length = 20)
    @Builder.Default
    private VirusScanStatus virusScanStatus = VirusScanStatus.PENDING;

    @Column(name = "virus_scan_detail", columnDefinition = "text")
    private String virusScanDetail;

    // ─── Aggregate counters ───────────────────────────────────────────────────

    @Column(name = "total_tables_found")
    @Builder.Default
    private Integer totalTablesFound = 0;

    @Column(name = "total_tables_valid")
    @Builder.Default
    private Integer totalTablesValid = 0;

    @Column(name = "total_rows")
    @Builder.Default
    private Long totalRows = 0L;

    @Column(name = "total_valid_rows")
    @Builder.Default
    private Long totalValidRows = 0L;

    @Column(name = "total_invalid_rows")
    @Builder.Default
    private Long totalInvalidRows = 0L;

    @Column(name = "total_warnings")
    @Builder.Default
    private Integer totalWarnings = 0;

    /** Số rule BLOCKER bị vi phạm. > 0 thì overall_status = FAILED */
    @Column(name = "blocker_count")
    @Builder.Default
    private Integer blockerCount = 0;

    // ─── Timing ──────────────────────────────────────────────────────────────

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Thời gian validate tính bằng milliseconds */
    @Column(name = "duration_ms")
    private Integer durationMs;

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

    // ─── Relationships ────────────────────────────────────────────────────────

    @OneToMany(
        mappedBy      = "validationDatasetResult",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    @OrderBy("id ASC")
    @Builder.Default
    private List<ValidationTableResult> tableResults = new ArrayList<>();

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public boolean hasPassed() {
        return overallStatus == ValidationStatus.PASSED;
    }

    public boolean isBlocked() {
        return blockerCount != null && blockerCount > 0;
    }

    public boolean isVirusClean() {
        return virusScanStatus == VirusScanStatus.CLEAN;
    }

    public void addTableResult(ValidationTableResult tableResult) {
        tableResults.add(tableResult);
        tableResult.setValidationDatasetResult(this);
    }
}
