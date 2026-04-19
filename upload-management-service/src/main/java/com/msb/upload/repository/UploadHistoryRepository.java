package com.msb.upload.repository;

import com.msb.upload.model.UploadHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UploadHistoryRepository extends JpaRepository<UploadHistory, Long> {
    List<UploadHistory> findByUpload_UploadIdOrderByChangedAtDesc(String uploadId);
}
