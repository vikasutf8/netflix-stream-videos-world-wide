package com.netflix.videoservice.exception;

public class InvalidVideoStatusException extends ContentServiceException {
    public InvalidVideoStatusException(String msg) {
        super(msg, "INVALID_VIDEO_STATUS");
    }
}
