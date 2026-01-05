package com.enterprise.upload.controller;

import com.enterprise.upload.dto.*;
import com.enterprise.upload.model.Upload;
import com.enterprise.upload.service.UploadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {
    
    private final UploadService uploadService;
    
    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }
    
    @PostMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request,
            @RequestHeader("X-User-Id") String userId) {
        
        PresignedUrlResponse response = uploadService.generatePresignedUrl(
                request, userId
        );
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{uploadId}/confirm")
    public ResponseEntity<Void> confirmUpload(
            @PathVariable String uploadId) {
        
        uploadService.confirmUpload(uploadId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{uploadId}/status")
    public ResponseEntity<Upload> getUploadStatus(
            @PathVariable String uploadId) {
        
        Upload upload = uploadService.getUploadStatus(uploadId);
        return ResponseEntity.ok(upload);
    }
}
