package com.enterprise.upload.model;

import com.enterprise.upload.model.enums.RuleSeverity;
import com.enterprise.upload.model.enums.RuleType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Kết quả của từng validation rule đã chạy trên một {@link ValidationTableResult}.
 * Bảng append-only, không update.
 *
 * <p>Mapping: {@code upload_db.validation_rule_results}
 */
@Entity
@Table(
    schema = "upload_db",
    name   = "validation_rule_results"
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationRuleResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "table_result_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "validation_rule_results_table_result_id_fkey")
    )
    private ValidationTableResult validationTableResult;

    /**
     * FK tới {@link TableValidationRule} – nullable vì rule config
     * có thể đã bị xóa/deactivate sau khi validate.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name       = "validation_rule_id",
        foreignKey = @ForeignKey(name = "validation_rule_results_validation_rule_id_fkey")
    )
    private TableValidationRule validationRule;

    /** Snapshot rule_code tại thời điểm validate (không bị ảnh hưởng nếu config thay đổi) */
    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", columnDefinition = "upload_db.rule_type_enum")
    private RuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", columnDefinition = "upload_db.rule_severity")
    private RuleSeverity severity;

    @Column(name = "target_column", length = 200)
    private String targetColumn;

    // ─── Result ───────────────────────────────────────────────────────────────

    @Column(name = "passed", nullable = false)
    private Boolean passed;

    /** Số dòng vi phạm rule này */
    @Column(name = "affected_rows")
    @Builder.Default
    private Long affectedRows = 0L;

    /** % dòng vi phạm trên tổng số dòng của table */
    @Column(name = "affected_percent", precision = 5, scale = 2)
    private BigDecimal affectedPercent;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    /**
     * Tối đa 100 mẫu dòng vi phạm, dùng để debug.
     * e.g. [{"row": 5, "value": "ABCX", "expected": "10-digit number"}, ...]
     */
    @Type(JsonBinaryType.class)
    @Column(name = "error_samples", columnDefinition = "jsonb")
    private List<Map<String, Object>> errorSamples;

    /** Thời gian thực thi rule tính bằng milliseconds */
    @Column(name = "execution_ms")
    private Integer executionMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public boolean isBlocker() {
        return severity == RuleSeverity.BLOCKER;
    }

    public boolean isFailedBlocker() {
        return !passed && isBlocker();
    }
}
