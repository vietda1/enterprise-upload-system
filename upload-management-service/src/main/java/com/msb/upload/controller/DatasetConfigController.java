package com.msb.upload.controller;

import com.msb.upload.dto.response.ApiResponse;
import com.msb.upload.dto.response.DatasetConfigResponse;
import com.msb.upload.service.DatasetConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dataset-configs")
@RequiredArgsConstructor
@Tag(name = "Dataset Configuration", description = "APIs for dataset configuration management")
public class DatasetConfigController {

    private final DatasetConfigService datasetConfigService;

    @GetMapping
    @Operation(summary = "List all active dataset configurations")
    public ResponseEntity<ApiResponse<List<DatasetConfigResponse>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.ok(datasetConfigService.getAllActive()));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get dataset configuration by code")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DatasetConfigResponse>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(datasetConfigService.getByCode(code)));
    }
}
