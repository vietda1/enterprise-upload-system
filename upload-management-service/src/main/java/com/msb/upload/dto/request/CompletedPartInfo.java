package com.msb.upload.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CompletedPartInfo {

    @Min(value = 1, message = "partNumber must be >= 1")
    private int partNumber;

    @NotBlank(message = "etag is required")
    private String etag;
}