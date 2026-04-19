package com.msb.upload.model;

import com.msb.upload.model.base.AuditableEntity;
import com.msb.upload.model.enums.WriteMode;
// import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Định nghĩa một table/entity bên trong một {@link DatasetConfig}.
 * Một DatasetConfig có thể có nhiều DatasetTable.
 *
 * <p>Mapping: {@code upload_db.dataset_tables}
 */
@Entity
@Table(
    schema = "upload_db",
    name   = "dataset_tables",
    uniqueConstraints = @UniqueConstraint(
        name        = "dataset_tables_dataset_config_id_table_key_key",
        columnNames = {"dataset_config_id", "table_key"}
    )
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetTable extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name                 = "dataset_config_id",
        nullable             = false,
        foreignKey           = @ForeignKey(name = "dataset_tables_dataset_config_id_fkey")
    )
    private DatasetConfig datasetConfig;

    /** Mã định danh table trong dataset, e.g. declaration_header, account_detail */
    @Column(name = "table_key", nullable = false, length = 100)
    private String tableKey;

    @Column(name = "table_name", nullable = false, length = 300)
    private String tableName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /**
     * Đường dẫn để extract table từ file nguồn.
     * XML: XPath, e.g. {@code //NOIDUNG_DANHSACH_CTIET/ROW_CTIET}
     * JSON: JSONPath, e.g. {@code $.banking_export.accounts[*]}
     * CSV: NULL (toàn bộ file là 1 table)
     */
    @Column(name = "source_path", length = 500)
    private String sourcePath;

    /** Tên sheet (dành cho XLSX multi-sheet) */
    @Column(name = "source_sheet", length = 200)
    private String sourceSheet;

    @Column(name = "target_db_type", length = 50)
    private String targetDbType;

    @Column(name = "target_connection", length = 500)
    private String targetConnection;

    @Column(name = "target_table", length = 200)
    private String targetTable;

    @Column(name = "target_schema", length = 100)
    @Builder.Default
    private String targetSchema = "public";

    /**
     * Cách ghi dữ liệu vào target: APPEND | UPSERT | REPLACE | MERGE | TRUNCATE.
     *
     * @see WriteMode
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "write_mode", length = 20)
    @Builder.Default
    private WriteMode writeMode = WriteMode.APPEND;

    /**
     * Các cột làm key khi UPSERT, lưu dạng PostgreSQL text[].
     */
    // @Type(ListArrayType.class)
    // @Column(name = "upsert_keys", columnDefinition = "text[]")
    // private List<String> upsertKeys;

    /** Thứ tự ingest (quan trọng khi có FK giữa các tables) */
    @Column(name = "ingest_order")
    @Builder.Default
    private Integer ingestOrder = 1;

    /** Table có bắt buộc tồn tại trong file không */
    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = true;

    @Column(name = "min_row_count")
    @Builder.Default
    private Integer minRowCount = 0;

    @Column(name = "max_row_count")
    private Integer maxRowCount;

    @Column(name = "has_header_row")
    @Builder.Default
    private Boolean hasHeaderRow = true;

    @Column(name = "header_row_index")
    @Builder.Default
    private Integer headerRowIndex = 0;

    @Column(name = "data_start_row")
    @Builder.Default
    private Integer dataStartRow = 1;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 1;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // ─── Relationships ────────────────────────────────────────────────────────

    @OneToMany(
        mappedBy      = "datasetTable",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    @OrderBy("column_order ASC")
    @Builder.Default
    private List<TableColumn> columns = new ArrayList<>();

    @OneToMany(
        mappedBy      = "datasetTable",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    @OrderBy("check_order ASC")
    @Builder.Default
    private List<TableValidationRule> validationRules = new ArrayList<>();

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public void addColumn(TableColumn column) {
        columns.add(column);
        column.setDatasetTable(this);
    }

    public void addValidationRule(TableValidationRule rule) {
        validationRules.add(rule);
        rule.setDatasetTable(this);
    }
}
