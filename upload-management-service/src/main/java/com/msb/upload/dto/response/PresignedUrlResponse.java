package com.msb.upload.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class PresignedUrlResponse {
    private String uploadId;
    private String presignedUrl;
    private String objectKey;
    private String bucketName;
    private LocalDateTime expiresAt;
    private int expiryMinutes;
}
