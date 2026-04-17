package com.enterprise.upload.repository;

import com.enterprise.upload.model.IngestionJob;
import com.enterprise.upload.model.enums.IngestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IngestionJobRepository extends JpaRepository<IngestionJob, Integer> {
    Optional<IngestionJob> findByJobId(String jobId);
    List<IngestionJob> findByUpload_UploadIdOrderByCreatedAtDesc(String uploadId);
    Optional<IngestionJob> findFirstByUpload_UploadIdOrderByCreatedAtDesc(String uploadId);
}
