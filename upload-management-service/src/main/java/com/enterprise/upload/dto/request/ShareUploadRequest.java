package com.enterprise.upload.dto.request;

import com.enterprise.upload.model.enums.SharePermission;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ShareUploadRequest {
    private String sharedWithUserId;
    private String sharedWithDepartment;

    @NotNull(message = "permission is required")
    private SharePermission permission;

    private LocalDateTime expiresAt;
}
