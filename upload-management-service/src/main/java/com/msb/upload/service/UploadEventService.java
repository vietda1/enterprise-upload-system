package com.msb.upload.service;

import com.msb.upload.model.Upload;
import java.util.Map;

public interface UploadEventService {
    void record(Upload upload, String eventType, String eventSource,
                String actorId, String actorRole,
                String fromStatus, String toStatus,
                String message, Map<String, Object> metadata, String ipAddress);
}
