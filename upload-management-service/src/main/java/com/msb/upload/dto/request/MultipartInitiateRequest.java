package com.msb.upload.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.Map;

@Data
public class MultipartInitiateRequest {

    @NotBlank(message = "fileName is required")
    @Size(max = 500, message = "fileName too long")
    private String fileName;

    @NotBlank(message = "fileType is required")
    private String fileType;

    @NotNull(message = "fileSize is required")
    @Min(value = 1, message = "fileSize must be > 0")
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

    /** Part size in MB. Min 5 MB (S3 minimum), max 100 MB. Default 10 MB. */
    @Min(value = 5, message = "partSizeMb must be at least 5")
    @Max(value = 100, message = "partSizeMb must be at most 100")
    private int partSizeMb = 10;

    /** SHA-256 hoặc MD5 của toàn bộ file, do frontend tính trước khi upload. Dùng để dedup. */
    private String checksum;

    /** SHA256 | MD5. Mặc định SHA256 nếu không truyền. */
    private String checksumAlgorithm;
}