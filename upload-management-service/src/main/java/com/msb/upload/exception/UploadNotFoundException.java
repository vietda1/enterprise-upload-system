package com.msb.upload.exception;

public class UploadNotFoundException extends RuntimeException {
    public UploadNotFoundException(String uploadId) {
        super("Upload not found: " + uploadId);
    }
}
