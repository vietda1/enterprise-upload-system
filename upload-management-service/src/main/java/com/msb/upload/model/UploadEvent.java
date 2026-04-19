package com.msb.upload.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

// import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Audit trail / event log cho mỗi thay đổi trạng thái của một Upload.
 * Bảng chỉ ghi thêm (append-only), không update.
 *
 * <p>Mapping: {@code upload_db.upload_events}
 */
@Entity
@Table(
    schema = "upload_db",
    name   = "upload_events"
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name       = "upload_id",
        referencedColumnName = "upload_id",
        nullable   = false,
        foreignKey = @ForeignKey(name = "upload_events_upload_id_fkey")
    )
    private Upload upload;

    /**
     * Loại sự kiện, e.g.
     * PRESIGNED_URL_REQUESTED, FILE_UPLOAD_COMPLETED, VALIDATION_STARTED,
     * VALIDATION_COMPLETED, PENDING_APPROVAL_NOTIFIED, APPROVED, REJECTED,
     * INGESTION_STARTED, INGESTION_COMPLETED, INGESTION_FAILED
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /** Service/component tạo event, e.g. FRONTEND, VALIDATION_SERVICE, INGEST_SERVICE */
    @Column(name = "event_source", length = 100)
    private String eventSource;

    @Column(name = "actor_id", length = 100)
    private String actorId;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(name = "from_status", length = 50)
    private String fromStatus;

    @Column(name = "to_status", length = 50)
    private String toStatus;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
