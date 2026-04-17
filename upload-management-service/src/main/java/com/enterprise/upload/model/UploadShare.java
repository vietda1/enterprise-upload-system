package com.enterprise.upload.model;

import com.enterprise.upload.model.enums.SharePermission;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Chia sẻ quyền truy cập Upload cho user hoặc department khác.
 *
 * <p>Ràng buộc DB: {@code chk_share_target} – phải có đúng một trong hai:
 * {@code shared_with_user_id} hoặc {@code shared_with_department}.
 *
 * <p>Mapping: {@code upload_db.upload_shares}
 */
@Entity
@Table(
    schema = "upload_db",
    name   = "upload_shares"
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name                 = "upload_id",
        referencedColumnName = "upload_id",
        nullable             = false,
        foreignKey           = @ForeignKey(name = "fk_upload_shares_upload")
    )
    private Upload upload;

    /**
     * User được chia sẻ.
     * Phải có đúng một trong hai: {@code sharedWithUserId} hoặc {@code sharedWithDepartment}.
     */
    @Column(name = "shared_with_user_id", length = 255)
    private String sharedWithUserId;

    /**
     * Department được chia sẻ.
     * Phải có đúng một trong hai: {@code sharedWithUserId} hoặc {@code sharedWithDepartment}.
     */
    @Column(name = "shared_with_department", length = 100)
    private String sharedWithDepartment;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 50)
    @Builder.Default
    private SharePermission permission = SharePermission.READ;

    @Column(name = "shared_by", nullable = false, length = 255)
    private String sharedBy;

    @Column(name = "shared_at", nullable = false, updatable = false)
    private LocalDateTime sharedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (sharedAt == null) {
            sharedAt = LocalDateTime.now();
        }
    }

    // ─── Business helpers ────────────────────────────────────────────────────

    /** Kiểm tra chia sẻ với user cụ thể hay department */
    public boolean isSharedWithUser() {
        return sharedWithUserId != null;
    }

    public boolean isSharedWithDepartment() {
        return sharedWithDepartment != null;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
