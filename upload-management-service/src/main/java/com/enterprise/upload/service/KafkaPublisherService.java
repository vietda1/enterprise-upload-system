package com.enterprise.upload.service;

import com.enterprise.upload.model.Upload;

public interface KafkaPublisherService {
    void publishUploadCompleted(Upload upload);
    void publishUploadStatusChanged(Upload upload, String previousStatus);
}
