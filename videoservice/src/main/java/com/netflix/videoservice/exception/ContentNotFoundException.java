package com.netflix.videoservice.exception;


import java.util.UUID;

public class ContentNotFoundException extends ContentServiceException {
    public ContentNotFoundException(UUID id) {
        super("Content not found with id: " + id, "CONTENT_NOT_FOUND");
    }
}