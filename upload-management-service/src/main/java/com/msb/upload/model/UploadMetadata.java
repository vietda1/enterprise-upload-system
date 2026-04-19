package com.msb.upload.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Key-value metadata mở rộng cho Upload.
 * Dùng khi cần lưu các thuộc tính động không cố định schema.
 *
 * <p>Mapping: {@code upload_db.upload_metadata}
 */
@Entity
@Table(
    schema = "upload_db",
    name   = "upload_metadata"
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadMetadata {

    @EmbeddedId
    private UploadMetadataId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("uploadId")
    @JoinColumn(
        name                 = "upload_id",
        referencedColumnName = "upload_id",
        nullable             = false,
        foreignKey           = @ForeignKey(name = "fk_upload_metadata_upload")
    )
    private Upload upload;

    @Column(name = "meta_value", columnDefinition = "text")
    private String metaValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ─── Composite PK ────────────────────────────────────────────────────────

    @Embeddable
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class UploadMetadataId implements Serializable {

        @Column(name = "upload_id", length = 36)
        private String uploadId;

        @Column(name = "meta_key", length = 255)
        private String metaKey;
    }
}
