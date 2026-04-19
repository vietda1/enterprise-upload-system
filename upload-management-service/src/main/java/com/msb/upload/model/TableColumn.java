package com.msb.upload.model;

import com.msb.upload.model.enums.ColDataType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Schema chi tiết từng cột của một {@link DatasetTable}.
 *
 * <p>Mapping: {@code upload_db.table_columns}
 */
@Entity
@Table(
    schema = "upload_db",
    name   = "table_columns",
    uniqueConstraints = @UniqueConstraint(
        name        = "table_columns_dataset_table_id_source_column_key",
        columnNames = {"dataset_table_id", "source_column"}
    )
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "dataset_table_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "table_columns_dataset_table_id_fkey")
    )
    private DatasetTable datasetTable;

    /** Tên cột trong file nguồn (header row) */
    @Column(name = "source_column", nullable = false, length = 200)
    private String sourceColumn;

    /** Tên cột trong DB đích (NULL = dùng source_column) */
    @Column(name = "target_column", length = 200)
    private String targetColumn;

    @Column(name = "display_name", length = 300)
    private String displayName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /**
     * Kiểu dữ liệu – ánh xạ từ PostgreSQL custom type {@code upload_db.col_data_type}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, columnDefinition = "upload_db.col_data_type")
    @Builder.Default
    private ColDataType dataType = ColDataType.STRING;

    @Column(name = "max_length")
    private Integer maxLength;

    /** Tổng số chữ số (DECIMAL) */
    @Column(name = "precision")
    private Integer precision;

    /** Số chữ số thập phân (DECIMAL) */
    @Column(name = "scale")
    private Integer scale;

    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = false;

    @Column(name = "is_primary_key")
    @Builder.Default
    private Boolean isPrimaryKey = false;

    @Column(name = "is_unique")
    @Builder.Default
    private Boolean isUnique = false;

    @Column(name = "is_indexed")
    @Builder.Default
    private Boolean isIndexed = false;

    /** Cột chứa dữ liệu nhạy cảm cần masking trước khi log/hiển thị */
    @Column(name = "requires_masking")
    @Builder.Default
    private Boolean requiresMasking = false;

    @Column(name = "default_value", columnDefinition = "text")
    private String defaultValue;

    /**
     * Expression transform khi ingest, e.g.
     * {@code UPPER(value)}, {@code TO_DATE(value, 'DD/MM/YYYY')}, {@code TRIM(value)}
     */
    @Column(name = "transform_expression", columnDefinition = "text")
    private String transformExpression;

    @Column(name = "column_order")
    @Builder.Default
    private Integer columnOrder = 1;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
