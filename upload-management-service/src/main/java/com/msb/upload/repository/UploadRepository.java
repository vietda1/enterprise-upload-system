package com.msb.upload.repository;

import com.msb.upload.model.Upload;
import com.msb.upload.model.enums.UploadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadRepository extends JpaRepository<Upload, UUID> {

    Optional<Upload> findByUploadId(String uploadId);
    boolean existsByUploadId(String uploadId);

    Page<Upload> findByUserIdAndDeletedAtIsNull(String userId, Pageable pageable);
    Page<Upload> findByDepartmentIdAndDeletedAtIsNull(String departmentId, Pageable pageable);
    Page<Upload> findByDeletedAtIsNull(Pageable pageable);

    @Query("""
        SELECT u FROM Upload u
        WHERE u.deletedAt IS NULL
          AND (:userId IS NULL OR u.userId = :userId)
          AND (:departmentId IS NULL OR u.departmentId = :departmentId)
          AND (:status IS NULL OR u.status = :status)
          AND (:datasetType IS NULL OR u.datasetType = :datasetType)
          AND (:from IS NULL OR u.createdAt >= :from)
          AND (:to IS NULL OR u.createdAt <= :to)
    """)
    Page<Upload> search(
        @Param("userId")       String userId,
        @Param("departmentId") String departmentId,
        @Param("status")       UploadStatus status,
        @Param("datasetType")  String datasetType,
        @Param("from")         LocalDateTime from,
        @Param("to")           LocalDateTime to,
        Pageable pageable
    );

    @Query("SELECT u FROM Upload u WHERE u.status = com.msb.upload.model.enums.UploadStatus.PENDING AND u.presignedUrlExpiresAt < :now AND u.deletedAt IS NULL")
    List<Upload> findExpiredPendingUploads(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Upload u SET u.status = com.msb.upload.model.enums.UploadStatus.EXPIRED WHERE u.status = com.msb.upload.model.enums.UploadStatus.PENDING AND u.presignedUrlExpiresAt < :now")
    int expireStaleUploads(@Param("now") LocalDateTime now);

    long countByUserIdAndStatus(String userId, UploadStatus status);
    long countByDepartmentIdAndStatus(String departmentId, UploadStatus status);

    /**
     * Tìm file trùng trong cùng department + dataset type dựa trên checksum.
     * Chỉ kiểm tra các upload chưa ở trạng thái terminal (DELETED/EXPIRED/REJECTED/FAILED).
     * Scope dedup: intra-department — khác department KHÔNG bị block.
     */
    @Query("""
        SELECT u FROM Upload u
        WHERE u.checksum = :checksum
          AND u.departmentId = :departmentId
          AND u.datasetType = :datasetType
          AND u.status NOT IN :excludedStatuses
          AND u.deletedAt IS NULL
        ORDER BY u.createdAt DESC
    """)
    List<Upload> findDuplicates(
        @Param("checksum")         String checksum,
        @Param("departmentId")     String departmentId,
        @Param("datasetType")      String datasetType,
        @Param("excludedStatuses") List<UploadStatus> excludedStatuses,
        Pageable pageable
    );
}
