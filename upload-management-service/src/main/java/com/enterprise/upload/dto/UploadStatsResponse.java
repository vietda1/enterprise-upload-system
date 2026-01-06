package com.enterprise.upload.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadStatsResponse {
    private Long totalUploads;
    private Long pendingUploads;
    private Long completedUploads;
    private Long failedUploads;
    private Long totalSize;
    private String formattedSize;
}