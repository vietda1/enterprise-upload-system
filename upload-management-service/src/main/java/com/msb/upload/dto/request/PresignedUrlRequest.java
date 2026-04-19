package com.msb.upload.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Map;

@Data
public class PresignedUrlRequest {

    @NotBlank(message = "fileName is required")
    @Size(max = 500, message = "fileName too long")
    private String fileName;

    @NotBlank(message = "fileType is required")
    private String fileType;

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

    /** SHA-256 hoặc MD5 của toàn bộ file, do frontend tính trước khi upload. Dùng để dedup. */
    private String checksum;

    /** SHA256 | MD5. Mặc định SHA256 nếu không truyền. */
    private String checksumAlgorithm;
}
