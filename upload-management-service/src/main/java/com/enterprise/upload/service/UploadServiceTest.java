package com.enterprise.upload.service;

import com.enterprise.upload.dto.*;
import com.enterprise.upload.model.*;
import com.enterprise.upload.repository.UploadRepository;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {
    
    @Mock
    private UploadRepository uploadRepository;
    
    @Mock
    private MinioService minioService;
    
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @InjectMocks
    private UploadService uploadService;
    
    private PresignedUrlRequest request;
    private Upload upload;
    private String testId = "test-id";
    
    @BeforeEach
    void setUp() {
        // Setup test data
        UploadMetadata metadata = new UploadMetadata();
        metadata.setDepartment("finance");
        metadata.setAccessLevel("PRIVATE");
        metadata.setDatasetType("CSV_TRANSACTION");
        metadata.setTargetDatabase("postgresql://localhost:5432/transactions");
        metadata.setAutoIngest(true);
        
        request = new PresignedUrlRequest();
        request.setFileName("test.csv");
        request.setFileType("text/csv");
        request.setMetadata(metadata);
        
        upload = new Upload();
        upload.setId(testId);
        upload.setUserId("user-123");
        upload.setFileName("test.csv");
        upload.setStatus(UploadStatus.PENDING);
    }
    
    @Test
    void testGeneratePresignedUrl() {
        // Arrange
        when(minioService.generatePresignedUrl(anyString(), anyString()))
            .thenReturn("http://minio:9000/presigned-url");
        when(uploadRepository.save(any(Upload.class)))
            .thenReturn(upload);
        
        // Act
        PresignedUrlResponse response = uploadService.generatePresignedUrl(request, "user-123");
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.getUploadId());
        assertNotNull(response.getPresignedUrl());
        assertEquals(3600, response.getExpirySeconds());
        
        verify(uploadRepository, times(1)).save(any(Upload.class));
        verify(minioService, times(1)).generatePresignedUrl(anyString(), anyString());
    }
    
    @Test
    void testConfirmUpload() {
        // Arrange
        when(uploadRepository.findById(testId))
            .thenReturn(Optional.of(upload));
        when(minioService.objectExists(anyString()))
            .thenReturn(true);
        
        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.size()).thenReturn(1024L);
        when(minioService.getObjectMetadata(anyString()))
            .thenReturn(stat);
        
        // Act
        uploadService.confirmUpload(testId);
        
        // Assert
        assertEquals(UploadStatus.UPLOADED, upload.getStatus());
        assertNotNull(upload.getUploadedAt());
        assertEquals(1024L, upload.getFileSize());
        
        verify(uploadRepository, times(1)).save(upload);
        verify(kafkaTemplate, times(1)).send(eq("upload.completed"), anyString(), anyMap());
    }
}