package com.msb.upload.service.impl;

import com.msb.upload.model.Upload;
import com.msb.upload.model.UploadEvent;
import com.msb.upload.repository.UploadEventRepository;
import com.msb.upload.service.UploadEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadEventServiceImpl implements UploadEventService {

    private final UploadEventRepository uploadEventRepository;

    @Override
    public void record(Upload upload, String eventType, String eventSource,
                       String actorId, String actorRole,
                       String fromStatus, String toStatus,
                       String message, Map<String, Object> metadata, String ipAddress) {
        try {
            UploadEvent event = UploadEvent.builder()
                .upload(upload)
                .eventType(eventType)
                .eventSource(eventSource)
                .actorId(actorId)
                .actorRole(actorRole)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .message(message)
                .metadata(metadata)
                .ipAddress(ipAddress)
                .build();
            uploadEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to record event {} for upload {}: {}", eventType, upload.getUploadId(), e.getMessage());
        }
    }
}
