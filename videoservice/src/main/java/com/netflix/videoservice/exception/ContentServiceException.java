package com.netflix.videoservice.exception;

public abstract class ContentServiceException extends RuntimeException {
    private final String errorCode;

    protected ContentServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}

//public class ContentNotFoundException extends ContentServiceException {
//    public ContentNotFoundException(UUID id) {
//        super("Content not found with id: " + id, "CONTENT_NOT_FOUND");
//    }
//}


//
