package com.msb.upload.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MultipartInitiateResponse {
    private String uploadId;
    private String s3UploadId;
    private String objectKey;
    private String bucketName;
    private long fileSizeBytes;
    private long partSizeBytes;
    private int totalParts;
    private List<PartUrlInfo> parts;
    private LocalDateTime expiresAt;
}