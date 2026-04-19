package com.msb.upload.repository;

import com.msb.upload.model.PresignedUrlLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PresignedUrlLogRepository extends JpaRepository<PresignedUrlLog, Integer> {
    List<PresignedUrlLog> findByUploadIdOrderByCreatedAtDesc(String uploadId);
}
