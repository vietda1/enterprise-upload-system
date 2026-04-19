package com.msb.upload.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Log các Kafka event đã publish bởi Upload Management Service.
 * Dùng để audit, replay event và xử lý dead-letter.
 *
 * <p>Mapping: {@code upload_db.kafka_event_log}
 */
@Entity
@Table(
    schema = "upload_db",
    name   = "kafka_event_log"
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KafkaEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Business key của Upload liên quan.
     * Nullable vì một số event hệ thống không gắn với upload cụ thể.
     */
    @Column(name = "upload_id", length = 100)
    private String uploadId;

    /** Kafka topic, e.g. upload.completed, validation.completed */
    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    @Column(name = "partition")
    private Integer partition;

    @Column(name = "offset")
    private Long offset;

    @Column(name = "key", length = 500)
    private String key;

    @Type(JsonBinaryType.class)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    /**
     * Trạng thái gửi: SENT | FAILED | PENDING_RETRY.
     */
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "SENT";

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "published_at", updatable = false)
    private LocalDateTime publishedAt;

    @PrePersist
    protected void onCreate() {
        if (publishedAt == null) {
            publishedAt = LocalDateTime.now();
        }
    }
}
