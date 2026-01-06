package com.enterprise.upload.repository;

import com.enterprise.upload.model.AccessLevel;
import com.enterprise.upload.model.Upload;
import com.enterprise.upload.model.UploadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UploadRepository extends JpaRepository<Upload, String>, 
                                         JpaSpecificationExecutor<Upload> {
    
    // Find by user
    List<Upload> findByUserId(String userId);
    
    Page<Upload> findByUserId(String userId, Pageable pageable);
    
    // Find by status
    List<Upload> findByStatus(UploadStatus status);
    
    Page<Upload> findByStatus(UploadStatus status, Pageable pageable);
    
    // Find by department
    List<Upload> findByDepartment(String department);
    
    Page<Upload> findByDepartment(String department, Pageable pageable);
    
    // Find by user and status
    List<Upload> findByUserIdAndStatus(String userId, UploadStatus status);
    
    Page<Upload> findByUserIdAndStatus(String userId, UploadStatus status, Pageable pageable);
    
    // Find by user and department
    List<Upload> findByUserIdAndDepartment(String userId, String department);
    
    Page<Upload> findByUserIdAndDepartment(String userId, String department, Pageable pageable);
    
    // Find by date range
    @Query("SELECT u FROM Upload u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<Upload> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Find by access level
    List<Upload> findByAccessLevel(AccessLevel accessLevel);
    
    // Complex queries
    @Query("SELECT u FROM Upload u WHERE u.userId = :userId " +
           "AND (:status IS NULL OR u.status = :status) " +
           "AND (:department IS NULL OR u.department = :department) " +
           "AND (:datasetType IS NULL OR u.datasetType = :datasetType)")
    Page<Upload> findByFilters(
        @Param("userId") String userId,
        @Param("status") UploadStatus status,
        @Param("department") String department,
        @Param("datasetType") String datasetType,
        Pageable pageable
    );
    
    // Find accessible uploads (private + department shared)
    @Query("SELECT u FROM Upload u WHERE " +
           "(u.userId = :userId OR " +
           "(u.department = :department AND u.accessLevel = 'SHARED') OR " +
           "u.accessLevel = 'PUBLIC') " +
           "AND u.status != 'DELETED'")
    Page<Upload> findAccessibleUploads(
        @Param("userId") String userId,
        @Param("department") String department,
        Pageable pageable
    );
    
    // Statistics
    @Query("SELECT COUNT(u) FROM Upload u WHERE u.userId = :userId AND u.status = :status")
    Long countByUserIdAndStatus(@Param("userId") String userId, @Param("status") UploadStatus status);
    
    @Query("SELECT COUNT(u) FROM Upload u WHERE u.department = :department")
    Long countByDepartment(@Param("department") String department);
    
    @Query("SELECT SUM(u.fileSize) FROM Upload u WHERE u.userId = :userId")
    Long getTotalSizeByUserId(@Param("userId") String userId);
    
    @Query("SELECT SUM(u.fileSize) FROM Upload u WHERE u.department = :department")
    Long getTotalSizeByDepartment(@Param("department") String department);
    
    // Update status
    @Modifying
    @Query("UPDATE Upload u SET u.status = :status, u.updatedAt = :updatedAt WHERE u.id = :id")
    int updateStatus(
        @Param("id") String id,
        @Param("status") UploadStatus status,
        @Param("updatedAt") LocalDateTime updatedAt
    );
    
    // Soft delete
    @Modifying
    @Query("UPDATE Upload u SET u.status = 'DELETED', u.updatedAt = :updatedAt WHERE u.id = :id")
    int softDelete(@Param("id") String id, @Param("updatedAt") LocalDateTime updatedAt);
    
    // Find old uploads for cleanup
    @Query("SELECT u FROM Upload u WHERE u.status = 'PENDING' AND u.createdAt < :cutoffDate")
    List<Upload> findExpiredPendingUploads(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Search by filename
    @Query("SELECT u FROM Upload u WHERE LOWER(u.fileName) LIKE LOWER(CONCAT('%', :filename, '%'))")
    Page<Upload> searchByFileName(@Param("filename") String filename, Pageable pageable);
    
    // Find by object key
    Optional<Upload> findByObjectKey(String objectKey);
    
    // Find recent uploads
    @Query("SELECT u FROM Upload u WHERE u.userId = :userId ORDER BY u.createdAt DESC")
    List<Upload> findRecentUploads(@Param("userId") String userId, Pageable pageable);
}