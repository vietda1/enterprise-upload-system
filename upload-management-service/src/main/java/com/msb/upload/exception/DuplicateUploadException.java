package com.msb.upload.exception;

import lombok.Getter;

@Getter
public class DuplicateUploadException extends RuntimeException {

    private final String existingUploadId;
    private final String existingStatus;
    private final String existingFileName;

    public DuplicateUploadException(String existingUploadId, String existingStatus, String existingFileName) {
        super("File already uploaded in this department. Existing uploadId: " + existingUploadId
            + " [" + existingStatus + "]");
        this.existingUploadId = existingUploadId;
        this.existingStatus = existingStatus;
        this.existingFileName = existingFileName;
    }
}