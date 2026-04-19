package com.msb.upload.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Log các presigned URL đã phát hành cho một Upload.
 * Dùng để audit, phát hiện URL bị dùng nhiều lần hoặc hết hạn.
 *
 * <p>Mapping: {@code upload_db.presigned_url_logs}
 */
@Entity
@Table(
    schema = "upload_db",
    name   = "presigned_url_logs"
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresignedUrlLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Business key của Upload. Không FK cứng để tránh vòng phụ thuộc
     * (log có thể tồn tại sau khi upload bị soft-delete).
     */
    @Column(name = "upload_id", nullable = false, length = 100)
    private String uploadId;

    /** PUT (upload lên MinIO) hoặc GET (download) */
    @Column(name = "url_type", length = 20)
    @Builder.Default
    private String urlType = "PUT";

    @Column(name = "object_key", length = 1000)
    private String objectKey;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** Thời điểm URL được sử dụng lần đầu */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "is_expired")
    @Builder.Default
    private Boolean isExpired = false;

    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public boolean hasExpired() {
        return isExpired || (expiresAt != null && LocalDateTime.now().isAfter(expiresAt));
    }

    public boolean hasBeenUsed() {
        return usedAt != null;
    }
}
