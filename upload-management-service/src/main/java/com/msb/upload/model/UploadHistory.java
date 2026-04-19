package com.msb.upload.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lịch sử thay đổi trạng thái của Upload.
 * Được ghi tự động bởi DB trigger {@code trg_log_upload_status_change}.
 * Bảng append-only, không update/delete.
 *
 * <p>Mapping: {@code upload_db.upload_history}
 */
@Entity
@Table(
    schema = "upload_db",
    name   = "upload_history"
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * FK tới {@link Upload#getUploadId()} (business key, không phải UUID).
     * Cascade DELETE từ uploads.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name                 = "upload_id",
        referencedColumnName = "upload_id",
        nullable             = false,
        foreignKey           = @ForeignKey(name = "fk_upload_history_upload")
    )
    private Upload upload;

    @Column(name = "old_status", length = 50)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 50)
    private String newStatus;

    @Column(name = "changed_by", length = 255)
    private String changedBy;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
}
