package com.msb.upload.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tag gắn với một Upload. Dùng để tìm kiếm, phân loại.
 * Ràng buộc unique (upload_id, tag) đảm bảo không tag trùng.
 *
 * <p>Mapping: {@code upload_db.upload_tags}
 */
@Entity
@Table(
    schema = "upload_db",
    name   = "upload_tags",
    uniqueConstraints = @UniqueConstraint(
        name        = "uk_upload_tag",
        columnNames = {"upload_id", "tag"}
    )
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name                 = "upload_id",
        referencedColumnName = "upload_id",
        nullable             = false,
        foreignKey           = @ForeignKey(name = "fk_upload_tags_upload")
    )
    private Upload upload;

    @Column(name = "tag", nullable = false, length = 100)
    private String tag;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
