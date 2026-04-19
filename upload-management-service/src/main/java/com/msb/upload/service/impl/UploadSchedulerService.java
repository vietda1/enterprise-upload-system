package com.msb.upload.service.impl;

import com.msb.upload.repository.UploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadSchedulerService {

    private final UploadRepository uploadRepository;

    /**
     * Every 5 minutes: mark PENDING uploads whose presigned URL has expired as EXPIRED.
     */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void expireStaleUploads() {
        int count = uploadRepository.expireStaleUploads(LocalDateTime.now());
        if (count > 0) {
            log.info("Expired {} stale PENDING uploads", count);
        }
    }
}
