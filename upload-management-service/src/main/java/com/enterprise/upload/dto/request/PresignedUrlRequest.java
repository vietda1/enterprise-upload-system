package com.enterprise.upload.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Map;

@Data
public class PresignedUrlRequest {

    @NotBlank(message = "fileName is required")
    @Size(max = 500, message = "fileName too long")
    private String fileName;

    @NotBlank(message = "fileType is required")
    private String fileType;

    private Long fileSize;
    private String mimeType;

    @NotBlank(message = "datasetType is required")
    private String datasetType;

    private String targetDatabase;
    private String targetTable;
    private String departmentId;
    private String description;
    private Map<String, Object> tags;
    private Map<String, Object> metadata;
}
