package com.netflix.contentservice.exception;

public class InvalidVideoStatusException extends ContentServiceException {
    public InvalidVideoStatusException(String msg) {
        super(msg, "INVALID_VIDEO_STATUS");
    }
}
