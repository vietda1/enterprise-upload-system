package com.msb.upload.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectUploadRequest {
    @NotBlank(message = "reason is required")
    private String reason;
}
