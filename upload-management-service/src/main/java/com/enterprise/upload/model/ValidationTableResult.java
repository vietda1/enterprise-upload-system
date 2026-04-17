package com.enterprise.upload.model;

import com.enterprise.upload.model.enums.ValidationTableStatus;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kết quả validate chi tiết cho từng table/entity được extract từ file.
 * Mỗi {@link ValidationDatasetResult} có nhiều ValidationTableResult.
 *
 * <p>Mapping: {@code upload_db.validation_table_results}
 */
@Entity
@Table(
    schema = "upload_db",
    name   = "validation_table_results"
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationTableResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "validation_result_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "validation_table_results_validation_result_id_fkey")
    )
    private ValidationDatasetResult validationDatasetResult;

    /**
     * FK tới {@link DatasetTable} – nullable vì table trong file có thể
     * không khớp với bất kỳ DatasetTable nào được cấu hình.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name       = "dataset_table_id",
        foreignKey = @ForeignKey(name = "validation_table_results_dataset_table_id_fkey")
    )
    private DatasetTable datasetTable;

    /** Khớp với {@link DatasetTable#getTableKey()} */
    @Column(name = "table_key", nullable = false, length = 100)
    private String tableKey;

    @Column(name = "table_name", length = 300)
    private String tableName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ValidationTableStatus status = ValidationTableStatus.PENDING;

    @Column(name = "quality_score", precision = 5, scale = 2)
    private BigDecimal qualityScore;

    // ─── Row counters ─────────────────────────────────────────────────────────

    @Column(name = "row_count")
    @Builder.Default
    private Long rowCount = 0L;

    @Column(name = "valid_rows")
    @Builder.Default
    private Long validRows = 0L;

    @Column(name = "invalid_rows")
    @Builder.Default
    private Long invalidRows = 0L;

    @Column(name = "skipped_rows")
    @Builder.Default
    private Long skippedRows = 0L;

    // ─── Rule counters ────────────────────────────────────────────────────────

    @Column(name = "rules_checked")
    @Builder.Default
    private Integer rulesChecked = 0;

    @Column(name = "rules_passed")
    @Builder.Default
    private Integer rulesPassed = 0;

    @Column(name = "rules_failed")
    @Builder.Default
    private Integer rulesFailed = 0;

    @Column(name = "blocker_count")
    @Builder.Default
    private Integer blockerCount = 0;

    @Column(name = "warning_count")
    @Builder.Default
    private Integer warningCount = 0;

    // ─── Diagnostic data ─────────────────────────────────────────────────────

    /**
     * Danh sách cột detect được trong file thực tế.
     * e.g. {@code {"columns": ["STT","MST","TEN_CHUTK"], "row_count": 141}}
     */
    @Type(JsonBinaryType.class)
    @Column(name = "detected_columns", columnDefinition = "jsonb")
    private Map<String, Object> detectedColumns;

    /** Tối đa 5 dòng dữ liệu mẫu từ file */
    @Type(JsonBinaryType.class)
    @Column(name = "sample_data", columnDefinition = "jsonb")
    private List<Map<String, Object>> sampleData;

    /** Thống kê từng cột: null_count, unique_count, min, max, distinct_values... */
    @Type(JsonBinaryType.class)
    @Column(name = "column_stats", columnDefinition = "jsonb")
    private Map<String, Object> columnStats;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ─── Relationships ────────────────────────────────────────────────────────

    @OneToMany(
        mappedBy      = "validationTableResult",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    @OrderBy("id ASC")
    @Builder.Default
    private List<ValidationRuleResult> ruleResults = new ArrayList<>();

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public boolean hasPassed() {
        return status == ValidationTableStatus.PASSED;
    }

    public boolean isBlocked() {
        return blockerCount != null && blockerCount > 0;
    }

    public void addRuleResult(ValidationRuleResult ruleResult) {
        ruleResults.add(ruleResult);
        ruleResult.setValidationTableResult(this);
    }
}
