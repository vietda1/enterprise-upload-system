package com.enterprise.upload.dto;

import com.enterprise.upload.model.UploadStatus;
import lombok.Data;

@Data
public class UploadFilterRequest {
    private UploadStatus status;
    private String department;
    private String datasetType;
    private String searchTerm;
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
}