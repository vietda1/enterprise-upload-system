package com.enterprise.upload.repository;

import com.enterprise.upload.model.UploadEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UploadEventRepository extends JpaRepository<UploadEvent, Long> {
    List<UploadEvent> findByUpload_UploadIdOrderByCreatedAtAsc(String uploadId);
}
