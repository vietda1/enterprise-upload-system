package com.msb.upload.service;

import com.msb.upload.model.Upload;

public interface KafkaPublisherService {
    void publishUploadCompleted(Upload upload);
    void publishUploadStatusChanged(Upload upload, String previousStatus);
}
