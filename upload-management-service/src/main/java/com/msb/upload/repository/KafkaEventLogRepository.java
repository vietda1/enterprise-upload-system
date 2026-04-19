package com.msb.upload.repository;

import com.msb.upload.model.KafkaEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KafkaEventLogRepository extends JpaRepository<KafkaEventLog, Long> {
    List<KafkaEventLog> findByUploadIdOrderByPublishedAtDesc(String uploadId);
    List<KafkaEventLog> findByStatusAndRetryCountLessThan(String status, int max);
}
