package com.enterprise.upload.model;

import com.enterprise.upload.model.base.AuditableEntity;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Định nghĩa loại dataset tổng thể.
 * Một dataset có thể chứa nhiều {@link DatasetTable} (is_multi_table = true).
 *
 * <p>Mapping: {@code upload_db.dataset_configs}
 */
@Entity
@Table(
    schema = "upload_db",
    name   = "dataset_configs",
    uniqueConstraints = @UniqueConstraint(name = "dataset_configs_code_key", columnNames = "code")
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetConfig extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Mã định danh duy nhất, e.g. TT126_BANK_ACCOUNT, CORE_BANKING_EOD */
    @Column(name = "code", nullable = false, length = 100, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 300)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /**
     * Danh sách định dạng file được hỗ trợ, lưu dạng text (CSV,JSON,XML...).
     * Tách bằng dấu phẩy khi đọc/ghi.
     */
    @Column(name = "supported_formats", columnDefinition = "text")
    private String supportedFormats;

    @Column(name = "max_file_size_mb")
    @Builder.Default
    private Integer maxFileSizeMb = 100;

    @Column(name = "file_encoding", length = 20)
    @Builder.Default
    private String fileEncoding = "UTF-8";

    /** TRUE nếu 1 file chứa nhiều tables (XML, JSON nested, XLSX multi-sheet) */
    @Column(name = "is_multi_table")
    @Builder.Default
    private Boolean isMultiTable = false;

    /** XPath / JSONPath tới phần tử gốc, e.g. /HSoThueDTu/HSoKhaiThue */
    @Column(name = "root_path", length = 500)
    private String rootPath;

    /**
     * Loại cấu trúc file: FLAT | HIERARCHICAL | COLUMNAR | MULTI_SHEET.
     *
     * @see #STRUCTURE_TYPE_CHECK constraint trong DDL
     */
    @Column(name = "structure_type", length = 30)
    @Builder.Default
    private String structureType = "FLAT";

    @Column(name = "default_target_db_type", length = 100)
    private String defaultTargetDbType;

    @Column(name = "default_target_conn", length = 500)
    private String defaultTargetConn;

    @Column(name = "requires_approval")
    @Builder.Default
    private Boolean requiresApproval = false;

    /**
     * Danh sách role được phép approve, lưu dạng PostgreSQL text[].
     * Dùng hypersistence-utils {@link ListArrayType} để map.
     */
    @Type(ListArrayType.class)
    @Column(name = "approval_roles", columnDefinition = "text[]")
    @Builder.Default
    private List<String> approvalRoles = List.of("ROLE_DEPARTMENT_MANAGER");

    @Column(name = "requires_validate")
    @Builder.Default
    private Boolean requiresValidate = true;

    @Column(name = "retention_days")
    @Builder.Default
    private Integer retentionDays = 90;

    @Column(name = "is_sensitive")
    @Builder.Default
    private Boolean isSensitive = false;

    /** Văn bản pháp lý liên quan, e.g. TT126/2020 */
    @Column(name = "regulatory_ref", length = 200)
    private String regulatoryRef;

    @Column(name = "min_quality_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal minQualityScore = new BigDecimal("90.00");

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    // ─── Relationships ────────────────────────────────────────────────────────

    @OneToMany(
        mappedBy     = "datasetConfig",
        cascade      = CascadeType.ALL,
        orphanRemoval = true,
        fetch        = FetchType.LAZY
    )
    @OrderBy("display_order ASC")
    @Builder.Default
    private List<DatasetTable> tables = new ArrayList<>();

    // ─── Helper ──────────────────────────────────────────────────────────────

    public void addTable(DatasetTable table) {
        tables.add(table);
        table.setDatasetConfig(this);
    }

    public void removeTable(DatasetTable table) {
        tables.remove(table);
        table.setDatasetConfig(null);
    }
}
