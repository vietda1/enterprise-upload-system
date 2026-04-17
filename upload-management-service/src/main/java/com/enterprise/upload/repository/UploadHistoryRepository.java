package com.enterprise.upload.repository;

import com.enterprise.upload.model.UploadHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UploadHistoryRepository extends JpaRepository<UploadHistory, Long> {
    List<UploadHistory> findByUpload_UploadIdOrderByChangedAtDesc(String uploadId);
}
