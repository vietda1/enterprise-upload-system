package com.msb.upload.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CompleteMultipartRequest {

    @NotEmpty(message = "parts must not be empty")
    @Valid
    private List<CompletedPartInfo> parts;
}