package com.enterprise.upload.controller;

import com.enterprise.upload.dto.*;
import com.enterprise.upload.service.UploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UploadController {
    
    private final UploadService uploadService;
    
    @PostMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request,
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("Received presigned URL request from user: {}", userId);
        PresignedUrlResponse response = uploadService.generatePresignedUrl(request, userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{uploadId}/confirm")
    public ResponseEntity<Void> confirmUpload(
            @PathVariable String uploadId,
            @RequestHeader("X-User-Id") String userId) {
        
        log.info("Confirming upload: {} by user: {}", uploadId, userId);
        uploadService.confirmUpload(uploadId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{uploadId}/status")
    public ResponseEntity<UploadResponse> getUploadStatus(
            @PathVariable String uploadId,
            @RequestHeader("X-User-Id") String userId) {
        
        UploadResponse response = uploadService.getUploadStatus(uploadId, userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<Page<UploadResponse>> listUploads(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @ModelAttribute UploadFilterRequest filter) {
        
        Page<UploadResponse> uploads = uploadService.listUploads(userId, department, filter);
        return ResponseEntity.ok(uploads);
    }
    
    @GetMapping("/accessible")
    public ResponseEntity<Page<UploadResponse>> getAccessibleUploads(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Department") String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<UploadResponse> uploads = uploadService.getAccessibleUploads(
            userId, 
            department, 
            PageRequest.of(page, size)
        );
        return ResponseEntity.ok(uploads);
    }
    
    @DeleteMapping("/{uploadId}")
    public ResponseEntity<Void> deleteUpload(
            @PathVariable String uploadId,
            @RequestHeader("X-User-Id") String userId) {
        
        uploadService.deleteUpload(uploadId, userId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/stats")
    public ResponseEntity<UploadStatsResponse> getUploadStats(
            @RequestHeader("X-User-Id") String userId) {
        
        UploadStatsResponse stats = uploadService.getUploadStats(userId);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/{uploadId}/download-url")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @PathVariable String uploadId,
            @RequestHeader("X-User-Id") String userId) {
        
        String downloadUrl = uploadService.getDownloadUrl(uploadId, userId);
        return ResponseEntity.ok(Map.of("downloadUrl", downloadUrl));
    }
}