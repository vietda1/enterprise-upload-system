package com.msb.upload.model;

import com.msb.upload.model.base.AuditableEntity;
import com.msb.upload.model.enums.RuleSeverity;
import com.msb.upload.model.enums.RuleType;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.Map;

/**
 * Validation rule gắn với một {@link DatasetTable}.
 *
 * <p>Mapping: {@code upload_db.table_validation_rules}
 */
@Entity
@Table(
    schema = "upload_db",
    name   = "table_validation_rules",
    uniqueConstraints = @UniqueConstraint(
        name        = "table_validation_rules_dataset_table_id_rule_code_key",
        columnNames = {"dataset_table_id", "rule_code"}
    )
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableValidationRule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "dataset_table_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "table_validation_rules_dataset_table_id_fkey")
    )
    private DatasetTable datasetTable;

    /** Mã rule duy nhất trong table, e.g. CHK_MST_FORMAT */
    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    /**
     * Loại rule – ánh xạ từ PostgreSQL custom type {@code upload_db.rule_type_enum}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, columnDefinition = "upload_db.rule_type_enum")
    private RuleType ruleType;

    /**
     * Mức độ nghiêm trọng – ánh xạ từ PostgreSQL custom type {@code upload_db.rule_severity}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, columnDefinition = "upload_db.rule_severity")
    @Builder.Default
    private RuleSeverity severity = RuleSeverity.CRITICAL;

    /** Cột áp dụng rule (NULL = toàn bộ table) */
    @Column(name = "target_column", length = 200)
    private String targetColumn;

    /**
     * Các cột tham chiếu phụ (UNIQUE_COMPOSITE, FOREIGN_KEY, CROSS_TABLE_SUM).
     * Lưu dạng PostgreSQL text[].
     */
    @Type(ListArrayType.class)
    @Column(name = "ref_columns", columnDefinition = "text[]")
    private List<String> refColumns;

    /**
     * Tham số của rule theo dạng JSONB.
     * Cấu trúc tùy rule_type:
     * <ul>
     *   <li>NOT_NULL         : {@code {}}</li>
     *   <li>REGEX_MATCH      : {@code {"pattern": "^[0-9]{10}$"}}</li>
     *   <li>ALLOWED_VALUES   : {@code {"values": ["01","02"]}}</li>
     *   <li>RANGE_BETWEEN    : {@code {"min": 0, "max": 1000000}}</li>
     *   <li>MAX_NULL_PERCENT : {@code {"percent": 5}}</li>
     *   <li>FOREIGN_KEY      : {@code {"ref_table_key": "header", "ref_column": "id"}}</li>
     * </ul>
     */
    @Type(JsonBinaryType.class)
    @Column(name = "params", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> params = Map.of();

    /** Thông báo lỗi tùy chỉnh hiển thị khi rule fail */
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    /** Gợi ý cách sửa lỗi */
    @Column(name = "fix_hint", columnDefinition = "text")
    private String fixHint;

    /** TRUE = dừng validate ngay khi rule BLOCKER này fail */
    @Column(name = "stop_on_fail")
    @Builder.Default
    private Boolean stopOnFail = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /** Thứ tự kiểm tra (số nhỏ chạy trước) */
    @Column(name = "check_order")
    @Builder.Default
    private Integer checkOrder = 100;
}
