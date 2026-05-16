package com.netflix.videoservice.exception;

public class S3UploadException extends ContentServiceException {
    public S3UploadException(String message) {
        super(message, "S3_UPLOAD_FAILED");
    }
}

