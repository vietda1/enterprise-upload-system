package com.enterprise.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PresignedUrlRequest {
    
    @NotBlank(message = "File name is required")
    private String fileName;
    
    @NotBlank(message = "File type is required")
    private String fileType;
    
    @NotNull(message = "Metadata is required")
    private UploadMetadata metadata;
    
    // Getters and Setters
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public UploadMetadata getMetadata() { return metadata; }
    public void setMetadata(UploadMetadata metadata) { this.metadata = metadata; }
}