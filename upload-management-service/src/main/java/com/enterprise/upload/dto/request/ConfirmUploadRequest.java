package com.enterprise.upload.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmUploadRequest {
    @NotBlank(message = "etag is required")
    private String etag;
    private Long fileSize;
}
