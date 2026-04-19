package com.msb.upload.repository;

import com.msb.upload.model.ValidationDatasetResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ValidationDatasetResultRepository extends JpaRepository<ValidationDatasetResult, Long> {
    List<ValidationDatasetResult> findByUpload_UploadIdOrderByValidationRunDesc(String uploadId);
    Optional<ValidationDatasetResult> findFirstByUpload_UploadIdOrderByValidationRunDesc(String uploadId);
}
